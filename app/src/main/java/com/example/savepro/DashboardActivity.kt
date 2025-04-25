package com.example.savepro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var totalIncomeTextView: TextView
    private lateinit var totalExpenseTextView: TextView
    private lateinit var balanceTextView: TextView
    private lateinit var viewBalance: View
    private lateinit var viewExpense: View
    private lateinit var noTransactionsTextView: TextView
    private lateinit var transactions: MutableList<Transaction>
    private lateinit var budgetLimitTextView: TextView

    private var budgetLimit: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        listView = findViewById(R.id.listViewTransactions)
        totalIncomeTextView = findViewById(R.id.textViewTotalIncome)
        totalExpenseTextView = findViewById(R.id.textViewTotalExpense)
        balanceTextView = findViewById(R.id.textViewBalance)
        noTransactionsTextView = findViewById(R.id.textViewNoTransactions)
        viewBalance = findViewById(R.id.viewBalance)
        viewExpense = findViewById(R.id.viewExpense)
        budgetLimitTextView = findViewById(R.id.textViewBudgetLimit) // Initialize the Budget Limit TextView

        // Initialize the transaction list
        transactions = mutableListOf()

//        // Add some dummy transactions for testing
//        transactions.add(Transaction("Petrol bill Kandy", 23044.0, "Fuel", "25th Dec", "Expense"))
//        transactions.add(Transaction("Salary", 500000.0, "Income", "30th Dec", "Income"))

        transactions = loadTransactionsFromPreferences()

        // Set the adapter for the ListView
        val adapter = TransactionAdapter(this, transactions)
        listView.adapter = adapter

        // Set the "Add Transaction" button functionality
        findViewById<Button>(R.id.btnAddTransaction).setOnClickListener {
            showAddTransactionDialog(adapter)  // Pass the adapter to refresh the list
        }

        // Set the "Set Budget Limit" button functionality
        findViewById<Button>(R.id.btnSetBudgetLimit).setOnClickListener {
            showSetBudgetLimitDialog()  // Open the dialog to set budget limit
        }

        // Static values for testing (you can use actual values here)
        val totalIncome = transactions.filter { it.type == "Income" }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        // Display values in the UI
        totalIncomeTextView.text = "Total Income: LKR $totalIncome"
        totalExpenseTextView.text = "Total Expense: LKR $totalExpense"
        balanceTextView.text = "Balance: LKR $balance"

        // Update the graphical progress bars
        updateProgressBar(balance, totalExpense)

        // Check if there are any transactions
        if (transactions.isEmpty()) {
            noTransactionsTextView.visibility = View.VISIBLE
        } else {
            noTransactionsTextView.visibility = View.GONE
        }

        val pieChart = findViewById<SimplePieChartView>(R.id.simplePieChart)
        pieChart.income = totalIncome.toFloat()
        pieChart.expense = totalExpense.toFloat()
        pieChart.invalidate() // Refresh the chart// Refresh the chart


    }

    // Function to show Add Transaction Dialog
    private fun showAddTransactionDialog(adapter: TransactionAdapter) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_transaction, null)
        val titleEditText = dialogView.findViewById<EditText>(R.id.editTextTitle)
        val amountEditText = dialogView.findViewById<EditText>(R.id.editTextAmount)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val dateEditText = dialogView.findViewById<EditText>(R.id.editTextDate)
        val radioGroup: RadioGroup = dialogView.findViewById(R.id.radioGroupTransactionType)

        // Extended categories
        val incomeCategories = arrayOf("Salary", "Bonus", "Freelance", "Other", "Investments", "Gift")

        val expenseCategories = arrayOf("Fuel", "Food", "Transport", "Entertainment", "Medical", "Utilities", "Rent", "Shopping", "Education")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Transaction")
        builder.setView(dialogView)

        // Set the default date to today's date
        val currentDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        dateEditText.setText(currentDate)

        // Initialize spinner adapter with Expense categories by default
        var selectedCategory = "Other" // Default selected category
        var categoryAdapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_spinner_item, expenseCategories)
        categorySpinner.adapter = categoryAdapter

        // Handle radio button change (Income or Expense)
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val categories = when (checkedId) {
                R.id.radioIncome -> incomeCategories
                R.id.radioExpense -> expenseCategories
                else -> expenseCategories
            }
            // Set new category adapter based on selected transaction type
            categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
            categorySpinner.adapter = categoryAdapter

            // Set the previously selected category back if it still exists in the new categories list
            if (categories.contains(selectedCategory)) {
                val spinnerPosition = categories.indexOf(selectedCategory)
                categorySpinner.setSelection(spinnerPosition)
            } else {
                // If the previously selected category is not available in the new list, set it to "Other"
                categorySpinner.setSelection(categories.indexOf("Other"))
                selectedCategory = "Other"
            }
        }

        builder.setPositiveButton("Add") { _, _ ->
            val title = titleEditText.text.toString().trim()
            val amountText = amountEditText.text.toString().trim()
            val category = categorySpinner.selectedItem.toString()
            var date = dateEditText.text.toString().trim()

            // If date is empty, use the current date
            if (date.isEmpty()) {
                date = currentDate
            }

            if (title.isEmpty() || amountText.isEmpty() || category.isEmpty()) {
                Toast.makeText(this, "All fields must be filled out", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            // Add the transaction
            transactions.add(Transaction(title, amount, category, date, if (radioGroup.checkedRadioButtonId == R.id.radioIncome) "Income" else "Expense"))
            saveTransactionsToPreferences()
            adapter.notifyDataSetChanged()

            updateUI()

            // Update the UI for empty transactions
            if (transactions.isEmpty()) {
                noTransactionsTextView.visibility = View.VISIBLE
            } else {
                noTransactionsTextView.visibility = View.GONE
            }
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    // Function to show Set Budget Limit Dialog
    private fun showSetBudgetLimitDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_set_budget, null)
        val budgetLimitEditText = dialogView.findViewById<EditText>(R.id.editTextBudgetLimit)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set Budget Limit")
        builder.setView(dialogView)

        builder.setPositiveButton("Set") { _, _ ->
            val budgetText = budgetLimitEditText.text.toString().trim()
            if (budgetText.isEmpty()) {
                Toast.makeText(this, "Please enter a valid budget", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            // Set the budget limit and update the TextView
            budgetLimit = budgetText.toDouble()

            // Assume you have a method to get total expenses (replace it with the actual value)
            val totalExpense = 621763.0 // Example total expense value, replace with actual data

            // Calculate the percentage of the budget spent
            val percentageSpent = calculatePercentageSpent(budgetLimit, totalExpense)

            // Format values to display without decimals
            val formattedBudgetLimit = String.format("LKR %.0f", budgetLimit) // No decimal points for budget limit
            val formattedPercentageSpent = String.format("%d", percentageSpent) // No decimal points for percentage

            // Update the TextView with the formatted values
            budgetLimitTextView.text = "Budget Limit: $formattedBudgetLimit\n\n$formattedPercentageSpent% of the budget has been spent"
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    // Function to calculate the percentage spent of the budget (rounded to the nearest whole number)
    private fun calculatePercentageSpent(budgetLimit: Double, totalExpense: Double): Int {
        return if (budgetLimit > 0) {
            // Round the percentage to the nearest whole number
            Math.round((totalExpense / budgetLimit) * 100).toInt()
        } else {
            0 // If budgetLimit is 0 or less, return 0%
        }
    }

    private fun updateUI() {
        // Recalculate total income, total expense, balance
        val totalIncome = transactions.filter { it.type == "Income" }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
        val balance = totalIncome - totalExpense

        // Update TextViews for income, expense, balance
        totalIncomeTextView.text = "Total Income: LKR $totalIncome"
        totalExpenseTextView.text = "Total Expense: LKR $totalExpense"
        balanceTextView.text = "Balance: LKR $balance"

        // Update the pie chart
        val pieChart = findViewById<SimplePieChartView>(R.id.simplePieChart)
        pieChart.income = totalIncome.toFloat()
        pieChart.expense = totalExpense.toFloat()
        pieChart.invalidate() // Refresh the chart

        // Update the progress bars for balance and expenses
        updateProgressBar(balance, totalExpense)

        // Check if there are any transactions
        if (transactions.isEmpty()) {
            noTransactionsTextView.visibility = View.VISIBLE
        } else {
            noTransactionsTextView.visibility = View.GONE
        }
    }



    // Function to update the graphical progress bars for balance and expenses
    private fun updateProgressBar(balance: Double, totalExpense: Double) {
        val totalAmount = balance + totalExpense

        if (totalAmount == 0.0) {
            // Avoid division by zero and keep the bar balanced at 50% each
            viewBalance.layoutParams.width = 0
            viewExpense.layoutParams.width = 0
            return
        }

        // Calculate percentage of balance and expenses
        val balancePercentage = (balance / totalAmount).toFloat()
        val expensePercentage = (totalExpense / totalAmount).toFloat()

        // Adjust the width of the balance and expense sections based on percentage
        val barTotalWidth = 1000 // You can adjust this to your preference for width
        viewBalance.layoutParams.width = (balancePercentage * barTotalWidth).toInt()
        viewExpense.layoutParams.width = (expensePercentage * barTotalWidth).toInt()

        // Refresh the layout
        viewBalance.requestLayout()
        viewExpense.requestLayout()
    }

    private fun saveTransactionsToPreferences() {
        val sharedPreferences = getSharedPreferences("SaveProPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val transactionStrings = transactions.map {
            "${it.title}|${it.amount}|${it.category}|${it.date}|${it.type}"
        }

        editor.putStringSet("transactions", transactionStrings.toSet())
        editor.apply()
    }

    private fun loadTransactionsFromPreferences(): MutableList<Transaction> {
        val sharedPreferences = getSharedPreferences("SaveProPrefs", MODE_PRIVATE)
        val transactionSet = sharedPreferences.getStringSet("transactions", emptySet()) ?: emptySet()

        return transactionSet.mapNotNull {
            val parts = it.split("|")
            if (parts.size == 5) {
                val title = parts[0]
                val amount = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                val category = parts[2]
                val date = parts[3]
                val type = parts[4]
                Transaction(title, amount, category, date, type)
            } else null
        }.toMutableList()
    }
}



data class Transaction(val title: String, val amount: Double, val category: String, val date: String, val type: String)

class TransactionAdapter(private val context: DashboardActivity, private val transactions: MutableList<Transaction>) : BaseAdapter() {

    override fun getCount(): Int {
        return transactions.size
    }

    override fun getItem(position: Int): Any {
        return transactions[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.transaction_item, parent, false)

        val transaction = getItem(position) as Transaction

        val titleTextView: TextView = view.findViewById(R.id.transactionTitle)
        val amountTextView: TextView = view.findViewById(R.id.transactionAmount)
        val categoryTextView: TextView = view.findViewById(R.id.transactionCategory)
        val dateTextView: TextView = view.findViewById(R.id.transactionDate)

        titleTextView.text = transaction.title
        amountTextView.text = "LKR : ${transaction.amount}"
        categoryTextView.text = "Category : ${transaction.category}"
        dateTextView.text = "Date : ${transaction.date}"

        // Set text color based on the type of transaction
        if (transaction.type == "Income") {
            titleTextView.setTextColor(context.resources.getColor(android.R.color.holo_green_dark))
        } else {
            titleTextView.setTextColor(context.resources.getColor(android.R.color.holo_red_dark))
        }

        return view
    }
}
