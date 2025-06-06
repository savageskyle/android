package com.example.lab2;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.HashMap;
import java.util.Map;

public class InputFragment extends Fragment {

    private ExpandableListView expandableListView;
    private RadioGroup yearRadioGroup;
    private Button okButton;

    private String selectedBook = null;
    private String selectedYear = null;

    private int selectedGroupPosition = -1;
    private int selectedChildPosition = -1;
    private int lastCheckedRadioId = -1;

    private final String[] authors = {"J.K. Rowling", "George Orwell", "Jane Austen"};
    private final Map<String, String[]> books = new HashMap<>();
    private SimpleExpandableListAdapter adapter;

    // Interface for communication with the activity
    public interface OnInputListener {
        void onInputSubmitted(String book, String year);
    }

    private OnInputListener inputListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            inputListener = (OnInputListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnInputListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_input, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize book data
        books.put(authors[0], new String[]{"Harry Potter 1", "Harry Potter 2", "Harry Potter 3"});
        books.put(authors[1], new String[]{"1984", "Animal Farm"});
        books.put(authors[2], new String[]{"Pride and Prejudice", "Emma", "Sense and Sensibility"});

        // Find views
        expandableListView = view.findViewById(R.id.expandableListView);
        yearRadioGroup = view.findViewById(R.id.yearRadioGroup);
        okButton = view.findViewById(R.id.okButton);

        // Set up expandable list
        adapter = new SimpleExpandableListAdapter();
        expandableListView.setAdapter(adapter);

        expandableListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            if (groupPosition == selectedGroupPosition && childPosition == selectedChildPosition) {
                clearBookSelection();
                Toast.makeText(getActivity(), "Book selection cleared", Toast.LENGTH_SHORT).show();
            } else {
                String author = authors[groupPosition];
                String book = books.get(author)[childPosition];
                selectedBook = book + " - " + author;

                selectedGroupPosition = groupPosition;
                selectedChildPosition = childPosition;
                adapter.notifyDataSetChanged();

                Toast.makeText(getActivity(), "Selected: " + selectedBook, Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        // Set up radio button deselection
        setupRadioButtonDeselection();

        // Set up OK button
        okButton.setOnClickListener(v -> {
            if (selectedBook != null && selectedYear != null) {
                inputListener.onInputSubmitted(selectedBook, selectedYear);
            } else {
                Toast.makeText(getActivity(),
                        "Please select both a book and a publication year",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // Method to clear all selections (can be called from activity)
    public void clearAllSelections() {
        clearBookSelection();
        yearRadioGroup.clearCheck();
        selectedYear = null;
        lastCheckedRadioId = -1;
    }

    private void clearBookSelection() {
        selectedBook = null;
        selectedGroupPosition = -1;
        selectedChildPosition = -1;
        adapter.notifyDataSetChanged();
    }

    private void setupRadioButtonDeselection() {
        for (int i = 0; i < yearRadioGroup.getChildCount(); i++) {
            RadioButton radioButton = (RadioButton) yearRadioGroup.getChildAt(i);
            radioButton.setOnClickListener(view -> {
                int clickedId = view.getId();

                if (lastCheckedRadioId == clickedId) {
                    yearRadioGroup.clearCheck();
                    selectedYear = null;
                    lastCheckedRadioId = -1;
                    Toast.makeText(getActivity(), "Year selection cleared", Toast.LENGTH_SHORT).show();
                } else {
                    lastCheckedRadioId = clickedId;
                    selectedYear = ((RadioButton) view).getText().toString();
                }
            });
        }

        yearRadioGroup.setOnCheckedChangeListener(null);
    }

    private class SimpleExpandableListAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return authors.length;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return books.get(authors[groupPosition]).length;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return authors[groupPosition];
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return books.get(authors[groupPosition])[childPosition];
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            TextView textView = new TextView(getActivity());
            textView.setText(authors[groupPosition]);
            textView.setPadding(50, 30, 20, 30);
            textView.setTextSize(16);

            if (isExpanded) {
                textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_up_float, 0);
            } else {
                textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0);
            }

            return textView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            TextView textView = new TextView(getActivity());
            textView.setText(books.get(authors[groupPosition])[childPosition]);
            textView.setPadding(100, 25, 15, 25);
            textView.setTextSize(14);

            if (groupPosition == selectedGroupPosition && childPosition == selectedChildPosition) {
                textView.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.radiobutton_on_background, 0, 0, 0);
            } else {
                textView.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.radiobutton_off_background, 0, 0, 0);
            }
            textView.setCompoundDrawablePadding(10);

            return textView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }
}