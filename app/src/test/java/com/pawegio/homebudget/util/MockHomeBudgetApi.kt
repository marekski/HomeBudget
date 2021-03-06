package com.pawegio.homebudget.util

import com.pawegio.homebudget.HomeBudgetApi
import com.pawegio.homebudget.MonthlyBudget
import org.threeten.bp.Month
import pl.mareklangiewicz.smokk.smokk

class MockHomeBudgetApi : HomeBudgetApi {

    override val isSignedIn: Boolean get() = isSignInResult

    val signIn = smokk<Unit>()
    val signOut = smokk<Unit>()
    val getMonthlyBudget = smokk<Month, MonthlyBudget>()

    var isSignInResult = false

    override suspend fun signIn() = signIn.invoke()

    override suspend fun signOut() = signOut.invoke()

    override suspend fun getMonthlyBudget(month: Month): MonthlyBudget =
        getMonthlyBudget.invoke(month)
}
