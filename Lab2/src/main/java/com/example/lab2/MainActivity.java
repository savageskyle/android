package com.example.lab2;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity
        implements InputFragment.OnInputListener, ResultFragment.OnCancelListener {

    private InputFragment inputFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            // Create and add the input fragment
            inputFragment = new InputFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, inputFragment)
                    .commit();
        }
    }

    @Override
    public void onInputSubmitted(String book, String year) {
        // Create the result fragment with the selected data
        ResultFragment resultFragment = ResultFragment.newInstance(book, year);

        // Replace the input fragment with the result fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, resultFragment)
                .addToBackStack(null)  // Add to back stack so we can go back to the input fragment
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    @Override
    public void onCancelClicked() {
        // Pop back to the input fragment
        getSupportFragmentManager().popBackStack();

        // Clear selections in the input fragment
        if (inputFragment != null) {
            inputFragment.clearAllSelections();
        }
    }
}