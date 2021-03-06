package com.pawegio.homebudget.main

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.pawegio.homebudget.MainViewModel
import com.pawegio.homebudget.MonthlyBudget
import com.pawegio.homebudget.R
import com.pawegio.homebudget.util.currencyValue
import kotlinx.android.synthetic.main.main_fragment.*
import kotlinx.android.synthetic.main.month_summary_surface.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class MainFragment : Fragment(R.layout.main_fragment) {

    private val viewModel by sharedViewModel<MainViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.monthType.observe(this, Observer(::updateMonthType))
        viewModel.monthlyBudget.observe(this, Observer(::updateMonthlyBudget))
        viewModel.isLoading.observe(this, Observer(::updateProgress))
        subscribeToActions()
    }

    private fun subscribeToActions() {
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.mainActions.accept(MainAction.Refresh)
            swipeRefreshLayout.isRefreshing = false
        }
        prevMonthButton.setOnClickListener {
            viewModel.mainActions.accept(MainAction.SelectPrevMonth)
        }
        nextMonthButton.setOnClickListener {
            viewModel.mainActions.accept(MainAction.SelectNextMonth)
        }
        openSpreadsheetButton.setOnClickListener {
            viewModel.mainActions.accept(MainAction.OpenSpreadsheet)
        }
        moreButton.setOnClickListener { openPopupMenu() }
    }

    private fun openPopupMenu() {
        PopupMenu(requireContext(), moreButton).apply {
            menuInflater.inflate(R.menu.main_popup_menu, menu)
            setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.action_pick_document -> MainAction.PickDocumentAgain
                    R.id.action_sign_out -> MainAction.SignOut
                    else -> throw IllegalArgumentException()
                }.let(viewModel.mainActions::accept)
                true
            }
        }.show()
    }

    private fun updateMonthType(monthType: MonthType?) {
        prevMonthButton.isInvisible = monthType == MonthType.FIRST
        nextMonthButton.isInvisible = monthType == MonthType.LAST
    }

    private fun updateMonthlyBudget(monthlyBudget: MonthlyBudget?) {
        monthlyBudget?.let { (month, plannedIncomes, plannedExpenses, actualIncomes, actualExpenses, categories) ->
            monthHeaderView.text = month
            incomesTextView.text = actualIncomes.currencyValue
            plannedIncomesView.text = getString(R.string.of, plannedIncomes.currencyValue)
            expensesTextView.text = actualExpenses.currencyValue
            plannedExpensesView.text = getString(R.string.of, plannedExpenses.currencyValue)
            totalTextView.text = (actualIncomes - actualExpenses).currencyValue
            val incomes = categories.first()
            allIncomesLayout.removeAllViews()
            incomes.subcategories
                .filter { it.actual.toDouble() > 0.0 || it.planned.toDouble() > 0.0 }
                .forEach { subcategory ->
                    allIncomesLayout.addView(SubcategoryView(requireContext()).apply {
                        this.subcategory = subcategory
                    })
                }
            val allExpenses = categories.drop(1)
            allExpensesLayout.removeAllViews()
            allExpenses.forEach { category ->
                allExpensesLayout.addView(CategoryView(requireContext()).apply {
                    this.category = category
                })
            }
        }
    }

    private fun updateProgress(isLoading: Boolean?) {
        if (isLoading == true) {
            allIncomesLayout.removeAllViews()
            allExpensesLayout.removeAllViews()
        }
        incomesHeaderView.isVisible = isLoading == false
        expensesHeaderView.isVisible = isLoading == false
        monthlyBudgetProgressBar.isVisible = isLoading == true
    }
}
