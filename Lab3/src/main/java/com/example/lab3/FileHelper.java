package com.example.lab3;

import android.content.Context;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FileHelper {
    private static final String FILE_NAME = "books_data.txt";

    // Data class to represent a book record
    public static class BookRecord {
        private String book;
        private String year;
        private int id;  // Used to identify records for update/delete

        public BookRecord(String book, String year) {
            this.book = book;
            this.year = year;
            this.id = -1;  // Will be set when saving/loading
        }

        public BookRecord(String book, String year, int id) {
            this.book = book;
            this.year = year;
            this.id = id;
        }

        public String getBook() {
            return book;
        }

        public String getYear() {
            return year;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "Book: " + book + "\nPublication Year: " + year;
        }
    }

    // Save a record to file
    public static boolean saveRecord(Context context, BookRecord record) {
        List<BookRecord> records = getAllRecords(context);

        // Generate a new ID for the record
        int maxId = 0;
        for (BookRecord existingRecord : records) {
            if (existingRecord.getId() > maxId) {
                maxId = existingRecord.getId();
            }
        }
        record.setId(maxId + 1);

        records.add(record);
        return saveAllRecords(context, records);
    }

    // Get all records from file
    public static List<BookRecord> getAllRecords(Context context) {
        List<BookRecord> records = new ArrayList<>();

        try {
            FileInputStream fis = context.openFileInput(FILE_NAME);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);

            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("|")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 3) {
                        int id = Integer.parseInt(parts[0]);
                        String book = parts[1];
                        String year = parts[2];
                        records.add(new BookRecord(book, year, id));
                    }
                }
            }

            br.close();
            isr.close();
            fis.close();
        } catch (IOException e) {
            // File doesn't exist or other error - return empty list
        }

        return records;
    }

    // Save all records to file
    private static boolean saveAllRecords(Context context, List<BookRecord> records) {
        try {
            FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);

            for (BookRecord record : records) {
                String line = record.getId() + "|" + record.getBook() + "|" + record.getYear() + "\n";
                fos.write(line.getBytes());
            }

            fos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Update an existing record
    public static boolean updateRecord(Context context, BookRecord updatedRecord) {
        List<BookRecord> records = getAllRecords(context);

        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).getId() == updatedRecord.getId()) {
                records.set(i, updatedRecord);
                return saveAllRecords(context, records);
            }
        }

        return false;  // Record not found
    }

    // Delete a record
    public static boolean deleteRecord(Context context, int recordId) {
        List<BookRecord> records = getAllRecords(context);

        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).getId() == recordId) {
                records.remove(i);
                return saveAllRecords(context, records);
            }
        }

        return false;  // Record not found
    }

    // Check if any records exist
    public static boolean hasRecords(Context context) {
        return !getAllRecords(context).isEmpty();
    }
}