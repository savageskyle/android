package com.example.lab3;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder> {

    private List<FileHelper.BookRecord> records;
    private OnRecordEditListener editListener;
    private OnRecordDeleteListener deleteListener;

    // Interface for edit callback
    public interface OnRecordEditListener {
        void onRecordEdit(FileHelper.BookRecord record);
    }

    // Interface for delete callback
    public interface OnRecordDeleteListener {
        void onRecordDelete(FileHelper.BookRecord record);
    }

    public RecordAdapter(List<FileHelper.BookRecord> records,
                         OnRecordEditListener editListener,
                         OnRecordDeleteListener deleteListener) {
        this.records = records;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        FileHelper.BookRecord record = records.get(position);
        holder.bookTextView.setText(record.getBook());
        holder.yearTextView.setText("Publication Year: " + record.getYear());

        holder.editButton.setOnClickListener(v -> {
            if (editListener != null) {
                editListener.onRecordEdit(record);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onRecordDelete(record);
            }
        });
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class RecordViewHolder extends RecyclerView.ViewHolder {
        TextView bookTextView;
        TextView yearTextView;
        Button editButton;
        Button deleteButton;

        RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            bookTextView = itemView.findViewById(R.id.bookTextView);
            yearTextView = itemView.findViewById(R.id.yearTextView);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}