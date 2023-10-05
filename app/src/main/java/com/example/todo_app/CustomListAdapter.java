package com.example.todo_app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;

public class CustomListAdapter extends BaseAdapter {
    private final Context context;
    private List<String> tasksList;
    private final TaskClickListener taskClickListener;

    public CustomListAdapter(Context context, List<String> tasksList, TaskClickListener listener) {
        this.context = context;
        this.tasksList = tasksList;
        this.taskClickListener = listener;
    }

    @Override
    public int getCount() {
        return tasksList.size();
    }

    @Override
    public Object getItem(int position) {
        return tasksList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setTasks(List<String> tasks) {
        this.tasksList = tasks;
        notifyDataSetChanged();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View rowView = inflater.inflate(R.layout.list_item_layout, parent, false);

        TextView textView = rowView.findViewById(R.id.itemText);
        TextView dueTextView = rowView.findViewById(R.id.itemDueTime);
        TextView dueDateTextView = rowView.findViewById(R.id.itemDueDate);

        String fullTask = tasksList.get(position);
        String taskName = extractTaskName(fullTask);
        textView.setText(taskName);

        Log.d("TAG", "taskname "+taskName);
        String dueTime = extractDueTime(fullTask);
        dueTextView.setText("DueTime: " + dueTime);

        String dueDate = extractDueDate(fullTask);
        dueDateTextView.setText("DueDate: " + dueDate);

        CheckBox checkBox = rowView.findViewById(R.id.taskCheckBox);
        checkBox.setChecked(false);
        // Extract due date from the full task

          // Set the text for the due time TextView
        rowView.setOnClickListener(v -> {
            String selectedTask = tasksList.get(position);
            String[] taskParts = selectedTask.split(" - ");
            String taskDescription = (taskParts.length > 1) ? taskParts[1] : "No description available";

            taskClickListener.onTaskClick(taskName, taskDescription, position); // Pass the position
        });


        return rowView;
    }



    // Helper method to extract task name from the full task string
    // Helper method to extract task name from the full task string
    private String extractTaskName(String fullTask) {
        int separatorIndex = fullTask.indexOf(" (Due:");
        if (separatorIndex != -1) {
            return fullTask.substring(0, separatorIndex).trim();
        } else {
            return fullTask.trim();
        }
    }


    private String extractDueTime(String fullTask) {
        int startSeparatorIndex = fullTask.indexOf(" (Due:");
        int endSeparatorIndex = fullTask.indexOf("on");

        if (startSeparatorIndex != -1 && endSeparatorIndex != -1) {
            return fullTask.substring(startSeparatorIndex + 7, endSeparatorIndex).trim();
        } else {
            return "No due time";
        }
    }
    private String extractDueDate(String fullTask) {
        int startDueDateIndex = fullTask.indexOf("on ");
        int endSeparatorIndex = fullTask.indexOf(")");
        if (startDueDateIndex != -1) {
            return fullTask.substring(startDueDateIndex + 3, endSeparatorIndex).trim();
        } else {
            return "No due date";
        }
    }
}
