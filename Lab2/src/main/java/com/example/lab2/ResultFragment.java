package com.example.lab2;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ResultFragment extends Fragment {

    private TextView resultTextView;
    private Button cancelButton;
    private String resultText;

    // Interface for communication with the activity
    public interface OnCancelListener {
        void onCancelClicked();
    }

    private OnCancelListener cancelListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            cancelListener = (OnCancelListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnCancelListener");
        }
    }

    // Factory method to create a new instance
    public static ResultFragment newInstance(String book, String year) {
        ResultFragment fragment = new ResultFragment();
        Bundle args = new Bundle();
        args.putString("book", book);
        args.putString("year", year);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String book = getArguments().getString("book");
            String year = getArguments().getString("year");
            resultText = "Book: " + book + "\nPublication Year: " + year;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        resultTextView = view.findViewById(R.id.resultTextView);
        cancelButton = view.findViewById(R.id.cancelButton);

        // Set result text
        resultTextView.setText(resultText);

        // Set up cancel button
        cancelButton.setOnClickListener(v -> {
            cancelListener.onCancelClicked();
        });
    }
}