package com.pawegio.homebudget

import com.pawegio.homebudget.util.SpreadsheetLauncher
import com.pawegio.homebudget.util.SpreadsheetLauncherImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import org.koin.android.experimental.dsl.viewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.threeten.bp.Clock

@ExperimentalCoroutinesApi
@FlowPreview
val appModule = module {
    single<HomeBudgetRepository> { HomeBudgetRepositoryImpl(androidContext()) }
    single<HomeBudgetApi> { HomeBudgetApiImpl(androidContext(), get()) }
    single<Clock> { Clock.systemDefaultZone() }
    single<SpreadsheetLauncher> { SpreadsheetLauncherImpl(androidContext(), get()) }
    single<Navigator> { NavigatorImpl(androidApplication(), ::appNavController) }
    viewModel<MainViewModel>()
}
