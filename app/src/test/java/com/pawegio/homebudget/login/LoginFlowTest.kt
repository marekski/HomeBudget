package com.pawegio.homebudget.login

import com.jakewharton.rxrelay2.PublishRelay
import com.nhaarman.mockitokotlin2.*
import com.pawegio.homebudget.*
import com.pawegio.homebudget.util.MockHomeBudgetApi
import com.pawegio.homebudget.util.SuspendFunction
import com.pawegio.homebudget.util.ToastNotifier
import io.kotlintest.shouldBe
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class LoginFlowTest : FlowSpec({
    "On login flow" - {
        val actions = PublishRelay.create<LoginAction>()
        val repository = mock<HomeBudgetRepository>()
        val api = MockHomeBudgetApi()
        val initPickerFlow = mock<SuspendFunction<Unit>>()
        val initMainFlow = mock<SuspendFunction<Unit>>()
        val navigator = mock<Navigator>()
        val toastNotifier = mock<ToastNotifier>()

        val flow = launch(start = CoroutineStart.LAZY) {
            @Suppress("EXPERIMENTAL_API_USAGE")
            LoginFlow(
                actions,
                repository,
                api,
                toastNotifier,
                initPickerFlow::invokeSuspend,
                initMainFlow::invokeSuspend,
                navigator
            )
        }

        "on user not signed in" - {
            api.isSignInResult = false
            flow.start()

            "do not sign in" {
                api.signIn.invocations shouldBe 0
            }

            "do not navigate to main screen" {
                verify(navigator, never()).navigate(R.id.action_loginFragment_to_mainFragment)
            }

            "do not init main flow" {
                verifyBlocking(initMainFlow, never()) { invokeSuspend() }
            }

            "on select sign in" - {
                actions.accept(LoginAction.SelectSignIn)

                "sign in" {
                    api.signIn.invocations shouldBe 1
                }

                "on sign in success" - {
                    api.isSignInResult = true
                    api.signIn.resume(Unit)

                    "navigate to picker screen" {
                        verify(navigator).navigate(R.id.action_loginFragment_to_pickerFragment)
                    }

                    "init picker flow" {
                        verifyBlocking(initPickerFlow) { invokeSuspend() }
                    }
                }

                "on sign in failure" - {
                    api.isSignInResult = false
                    api.signIn.resume(Unit)

                    "do not navigate to main screen" {
                        verify(navigator, never()).navigate(R.id.action_loginFragment_to_mainFragment)
                    }

                    "do not init main flow" {
                        verifyBlocking(initMainFlow, never()) { invokeSuspend() }
                    }
                }

                "on sign in failure with exception" - {
                    api.isSignInResult = false
                    api.signIn.resumeWithException(HomeBudgetApiException())

                    "do not navigate to main screen" {
                        verify(
                            navigator,
                            never()
                        ).navigate(R.id.action_loginFragment_to_mainFragment)
                    }

                    "do not init main flow" {
                        verifyBlocking(initMainFlow, never()) { invokeSuspend() }
                    }

                    "show sign in error" {
                        verify(toastNotifier).notify(R.string.sign_in_error)
                    }
                }
            }
        }

        "on user signed in" - {

            "on spreadsheet not picked" - {
                whenever(repository.spreadsheetId) doReturn null
                api.isSignInResult = true
                flow.start()

                "navigate to picker screen" {
                    verify(navigator).navigate(R.id.action_loginFragment_to_pickerFragment)
                }

                "init picker flow" {
                    verifyBlocking(initPickerFlow) { invokeSuspend() }
                }
            }

            "on spreadsheet picked" - {
                whenever(repository.spreadsheetId) doReturn "id"
                api.isSignInResult = true
                flow.start()

                "navigate to main screen" {
                    verify(navigator).navigate(R.id.action_loginFragment_to_mainFragment)
                }

                "init main flow" {
                    verifyBlocking(initMainFlow) { invokeSuspend() }
                }
            }
        }
    }
})
