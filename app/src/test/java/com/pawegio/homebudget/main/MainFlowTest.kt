package com.pawegio.homebudget.main

import androidx.lifecycle.MutableLiveData
import com.jakewharton.rxrelay2.PublishRelay
import com.jraska.livedata.test
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyBlocking
import com.pawegio.homebudget.*
import com.pawegio.homebudget.util.MockHomeBudgetApi
import com.pawegio.homebudget.util.SpreadsheetLauncher
import com.pawegio.homebudget.util.SuspendFunction
import com.pawegio.homebudget.util.createMonthlyBudget
import io.kotlintest.shouldBe
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import org.threeten.bp.Clock
import org.threeten.bp.Instant
import org.threeten.bp.Month
import org.threeten.bp.ZoneId
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class MainFlowTest : FlowSpec({
    "On main flow" - {
        val actions = PublishRelay.create<MainAction>()
        val monthlyBudget = MutableLiveData<MonthlyBudget>()
        val monthType = MutableLiveData<MonthType>()
        val isLoading = MutableLiveData<Boolean>()
        val repository = mock<HomeBudgetRepository>()
        val api = MockHomeBudgetApi()
        val spreadsheetLauncher = mock<SpreadsheetLauncher>()
        var clock = Clock.fixed(Instant.parse("2019-04-01T10:15:00.00Z"), ZoneId.systemDefault())
        val initPickerFlow = mock<SuspendFunction<Unit>>()
        val navigator = mock<Navigator>()

        val flow = launch(start = CoroutineStart.LAZY) {
            @Suppress("EXPERIMENTAL_API_USAGE")
            MainFlow(
                actions,
                monthType,
                monthlyBudget,
                isLoading,
                repository,
                api,
                spreadsheetLauncher,
                clock,
                initPickerFlow::invokeSuspend,
                navigator
            )
        }

        "on January" - {
            clock = Clock.fixed(Instant.parse("2019-01-01T10:15:00.00Z"), ZoneId.systemDefault())
            flow.start()

            "is first month" {
                monthType.test().assertValue(MonthType.FIRST)
            }
        }

        "on December" - {
            clock = Clock.fixed(Instant.parse("2019-12-01T10:15:00.00Z"), ZoneId.systemDefault())
            flow.start()

            "is last month" {
                monthType.test().assertValue(MonthType.LAST)
            }
        }

        flow.start()

        "is middle month" {
            monthType.test().assertValue(MonthType.MIDDLE)
        }

        "get monthly budget for current month" {
            api.getMonthlyBudget.invocations shouldBe listOf(Month.APRIL)
        }

        "show loader" {
            isLoading.test().assertValue(true)
        }

        "on monthly budget loaded with success" - {
            val loadedMonthlyBudget = createMonthlyBudget()
            api.getMonthlyBudget.resume(loadedMonthlyBudget)

            "hide loader" {
                isLoading.test().assertValue(false)
            }

            "update monthly budget" {
                monthlyBudget.test().assertValue(loadedMonthlyBudget)
            }

            "on refresh" - {
                actions.accept(MainAction.Refresh)

                "get refreshed monthly budget for current month" {
                    api.getMonthlyBudget.invocations.run {
                        count() shouldBe 2
                        last() shouldBe Month.APRIL
                    }
                }
            }

            "on open spreadsheet" - {
                actions.accept(MainAction.OpenSpreadsheet)

                "launch spreadsheet" {
                    verify(spreadsheetLauncher).launch()
                }
            }

            "on select prev month" - {
                actions.accept(MainAction.SelectPrevMonth)

                "get monthly budget for previous month" {
                    api.getMonthlyBudget.invocations.last() shouldBe Month.MARCH
                }
            }

            "on select next month" - {
                actions.accept(MainAction.SelectNextMonth)

                "get monthly budget for next month" {
                    api.getMonthlyBudget.invocations.last() shouldBe Month.MAY
                }
            }

            "on sign out" - {
                actions.accept(MainAction.SignOut)

                "clear spreadsheet id" {
                    verify(repository).spreadsheetId = null
                }

                "sign out" {
                    api.signOut.invocations shouldBe 1
                }

                "on sign out success" - {
                    api.signOut.resume(Unit)

                    "pop back stack" {
                        verify(navigator).popBackStack()
                    }
                }
            }
        }

        "on monthly budget load error" - {
            api.getMonthlyBudget.resumeWithException(HomeBudgetApiException())

            "hide loader" {
                isLoading.test().assertValue(false)
            }

            "show error dialog" {
                verify(navigator).navigate(R.id.action_mainFragment_to_loadErrorFragment)
            }

            "on try again" - {
                actions.accept(MainAction.TryAgain)

                "get refreshed monthly budget for current month" {
                    api.getMonthlyBudget.invocations.run {
                        count() shouldBe 2
                        last() shouldBe Month.APRIL
                    }
                }
            }

            "on pick document again" - {
                actions.accept(MainAction.PickDocumentAgain)

                "clear spreadsheet id" {
                    verify(repository).spreadsheetId = null
                }

                "navigate to picker screen" {
                    verify(navigator).navigate(R.id.action_mainFragment_to_pickerFragment)
                }

                "init picker flow" {
                    verifyBlocking(initPickerFlow) { invokeSuspend() }
                }
            }
        }
    }
})
