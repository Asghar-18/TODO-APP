package com.example.todo_app;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements TaskClickListener{

    private List<String> tasksList;
    private final Object tasksListLock = new Object();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private CustomListAdapter adapter;
    private List<String> originalTasksList;
    private EditText searchBar;

    private List<String> filteredTasks;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupAddTasksButton();
        setupSearchBar();
    }

    // Initialize views and adapters
    private void initializeViews() {
        searchBar = findViewById(R.id.searchBar);
        tasksList = new ArrayList<>();
        adapter = new CustomListAdapter(this, tasksList, this);

        ListView listView = findViewById(R.id.taskListView);
        listView.setAdapter(adapter);

        List<String> savedTasks = retrieveTasksFromSharedPreferences();
        tasksList.addAll(savedTasks);
        originalTasksList = new ArrayList<>(tasksList);
        filteredTasks = new ArrayList<>();
        adapter.notifyDataSetChanged();
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedTask = tasksList.get(position);
            String[] taskParts = selectedTask.split(" - ");
            String taskName = taskParts[0];
            String taskDescription = (taskParts.length > 1) ? taskParts[1] : "No description available";

            showTaskDescriptionDialog(taskName, taskDescription, position); // Pass the position
        });


    }

    // Set up the "Add Tasks" button
    private void setupAddTasksButton() {
        Button addTasksButton = findViewById(R.id.addTasksButton);
        addTasksButton.setOnClickListener(v -> showAddTaskDialog());
    }

    // Show the dialog to add a new task
    private void showAddTaskDialog() {
        View customView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);

        // Initialize views within the custom dialog view
        EditText taskEditText = customView.findViewById(R.id.taskEditText);
        TextInputEditText dueTimeEditText = customView.findViewById(R.id.dueTimeEditText);
        EditText taskDescriptionEditText = customView.findViewById(R.id.taskDescriptionEditText);
        TextInputEditText dueDateEditText = customView.findViewById(R.id.dueDateEditText);

        // Add a click listener to the dueDateEditText to open the date picker dialog
        dueDateEditText.setOnClickListener(v -> {
            showDatePickerDialog(dueDateEditText); // Pass the dueDateEditText
        });

        // Add a click listener to the dueTimeEditText to open the time picker dialog
        dueTimeEditText.setOnClickListener(v -> {
            showTimePickerDialog(dueTimeEditText); // Pass the dueTimeEditText
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(customView);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String newTask = taskEditText.getText().toString();
            String dueTime = Objects.requireNonNull(dueTimeEditText.getText()).toString();
            String taskDescription = taskDescriptionEditText.getText().toString();
            String dueDate = Objects.requireNonNull(dueDateEditText.getText()).toString();

            if (!isValidTimeFormat(dueTime) && !dueTime.isEmpty()) {
                Toast.makeText(MainActivity.this, "Invalid time format", Toast.LENGTH_SHORT).show();
            }

            if (!isValidDateFormat(dueDate) && !dueDate.isEmpty()) {
                Toast.makeText(MainActivity.this, "Invalid date format", Toast.LENGTH_SHORT).show();
            } else {
                addNewTask(newTask, dueTime, taskDescription, dueDate);
                dialog.dismiss(); // You need to use 'dialog' here to dismiss the AlertDialog
            }
        });

        AlertDialog dialog = builder.create(); // Declare 'dialog' here
        dialog.show();
    }

    // Add a new task to the list
    private void addNewTask(final String newTask, final String dueTime, final String taskDescription, final String dueDate) {
        executorService.submit(() -> {
            // Perform the heavy work here (e.g., formatting, adding tasks)
            String taskWithDetails = newTask + " (Due: " + dueTime + " on " + dueDate + ")";

            if (!taskDescription.isEmpty()) {
                taskWithDetails += " - " + taskDescription;
            }

            synchronized (tasksListLock) {
                tasksList.add(taskWithDetails);
                originalTasksList.add(taskWithDetails);
                saveTasksToSharedPreferences(tasksList);
            }

            runOnUiThread(() -> adapter.notifyDataSetChanged());
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    // Set up the search bar with a TextWatcher
    private void setupSearchBar() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                String query = charSequence.toString();
                filterTasks(query);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Do nothing
            }
        });
    }

    // Filter tasks based on the search query
    // Filter tasks based on the search query
    private void filterTasks(String query) {
        filteredTasks.clear();
        for (String task : originalTasksList) {
            if (task.toLowerCase().contains(query.toLowerCase())) {
                filteredTasks.add(task);
            }
        }
        adapter.setTasks(filteredTasks); // Update the filtered tasks in the adapter
        adapter.notifyDataSetChanged();
    }

    // Save tasks to SharedPreferences
    private void saveTasksToSharedPreferences(List<String> tasksList) {
        SharedPreferences sharedPreferences = getSharedPreferences("tasks_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt("taskCount", tasksList.size());

        for (int i = 0; i < tasksList.size(); i++) {
            editor.putString("task_" + i, tasksList.get(i));
        }

        editor.apply();
    }

    // Retrieve tasks from SharedPreferences
    private List<String> retrieveTasksFromSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("tasks_prefs", MODE_PRIVATE);
        int taskCount = sharedPreferences.getInt("taskCount", 0);

        List<String> savedTasks = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            String task = sharedPreferences.getString("task_" + i, "");
            savedTasks.add(task);
            Log.d("DEBUG", "Retrieved task: " + task);
        }

        return savedTasks;
    }

    private void showTaskDescriptionDialog(String taskName, String taskDescription, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(taskName)
                .setMessage(taskDescription)
                .setPositiveButton("Close", null)
                .setNegativeButton("Delete", (dialog, which) -> {
                    deleteTask(position);
                    dialog.dismiss();
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    @Override
    public void onTaskClick(String taskName, String taskDescription, int position) {
        showTaskDescriptionDialog(taskName, taskDescription, position); // Pass the position
    }


    private void deleteTask(int position) {
        if (position >= 0 && position < tasksList.size()) {
            tasksList.remove(position);
            originalTasksList.remove(position);
            adapter.notifyDataSetChanged();
            saveTasksToSharedPreferences(tasksList);
        }
    }

    // Add these methods to your MainActivity class


    private boolean isValidDateFormat(String inputDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dateFormat.setLenient(false); // This will ensure strict parsing

        try {
            dateFormat.parse(inputDate);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private boolean isValidTimeFormat(String inputTime) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        timeFormat.setLenient(false); // This will ensure strict parsing

        try {
            timeFormat.parse(inputTime);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }
    // Function to show the date picker dialog
    private void showDatePickerDialog(final TextInputEditText dueDateEditText) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Handle the selected date and update dueDateEditText
                    String formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                    dueDateEditText.setText(formattedDate);
                }, year, month, dayOfMonth);

        datePickerDialog.show();
    }
    // Function to show the dark-themed time picker dialog with AM/PM
    private void showTimePickerDialog(final TextInputEditText dueTimeEditText) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this,
                R.style.TimePickerDialogTheme, // Apply the custom style
                (view, selectedHour, selectedMinute) -> {
                    // Handle the selected time and update dueTimeEditText with AM/PM
                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s",
                            selectedHour % 12, selectedMinute, selectedHour < 12 ? "AM" : "PM");
                    dueTimeEditText.setText(formattedTime);
                }, hour, minute, false); // Set is24HourView to false to use 12-hour format

        timePickerDialog.show();
    }

}