package com.example.lab3;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StorageActivity extends AppCompatActivity {

    private RecyclerView recordsRecyclerView;
    private TextView emptyTextView;
    private Button backButton;
    private RecordAdapter adapter;
    private List<FileHelper.BookRecord> records;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage);

        // Find views
        recordsRecyclerView = findViewById(R.id.recordsRecyclerView);
        emptyTextView = findViewById(R.id.emptyTextView);
        backButton = findViewById(R.id.backButton);

        // Set up RecyclerView
        recordsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load records
        loadRecords();

        // Set up back button
        backButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload records when activity resumes (in case they were modified)
        loadRecords();
    }

    private void loadRecords() {
        records = FileHelper.getAllRecords(this);

        if (records.isEmpty()) {
            emptyTextView.setVisibility(View.VISIBLE);
            recordsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyTextView.setVisibility(View.GONE);
            recordsRecyclerView.setVisibility(View.VISIBLE);

            adapter = new RecordAdapter(records, this::editRecord, this::deleteRecord);
            recordsRecyclerView.setAdapter(adapter);
        }
    }

    private void editRecord(FileHelper.BookRecord record) {
        // Create a dialog to edit the record
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Record");

        // Inflate a custom layout for the dialog
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_record, null);
        EditText bookEditText = dialogView.findViewById(R.id.bookEditText);
        EditText yearEditText = dialogView.findViewById(R.id.yearEditText);

        // Pre-fill with existing data
        bookEditText.setText(record.getBook());
        yearEditText.setText(record.getYear());

        builder.setView(dialogView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newBook = bookEditText.getText().toString().trim();
            String newYear = yearEditText.getText().toString().trim();

            if (!newBook.isEmpty() && !newYear.isEmpty()) {
                FileHelper.BookRecord updatedRecord = new FileHelper.BookRecord(newBook, newYear, record.getId());
                if (FileHelper.updateRecord(this, updatedRecord)) {
                    Toast.makeText(this, "Record updated successfully", Toast.LENGTH_SHORT).show();
                    loadRecords();  // Reload to show changes
                } else {
                    Toast.makeText(this, "Error updating record", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    private void deleteRecord(FileHelper.BookRecord record) {
        // Confirm deletion
        new AlertDialog.Builder(this)
                .setTitle("Delete Record")
                .setMessage("Are you sure you want to delete this record?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (FileHelper.deleteRecord(this, record.getId())) {
                        Toast.makeText(this, "Record deleted successfully", Toast.LENGTH_SHORT).show();
                        loadRecords();  // Reload to show changes
                    } else {
                        Toast.makeText(this, "Error deleting record", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}