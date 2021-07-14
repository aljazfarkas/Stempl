package com.pora.stempl.ui.dashboard;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.andrognito.pinlockview.IndicatorDots;
import com.andrognito.pinlockview.PinLockListener;
import com.andrognito.pinlockview.PinLockView;
import com.pora.lib.PeopleEditModel;
import com.pora.stempl.AddPersonActivity;
import com.pora.stempl.ApplicationMy;
import com.pora.stempl.PeopleAdapter;
import com.pora.stempl.PeopleAdapterDashboard;
import com.pora.stempl.R;
import com.pora.stempl.ui.home.HomeFragment;
import com.pora.stempl.ui.home.HomeViewModel;

import java.time.LocalDateTime;

public class DashboardFragment extends Fragment implements PeopleAdapterDashboard.OnItemClickListener{
    public static final String TAG = DashboardFragment.class.getSimpleName();

    private ApplicationMy app;
    private PeopleAdapterDashboard adapter;
    private RecyclerView recyclerView;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;

    // Pin lock popup
    private PinLockView mPinLockView;
    private IndicatorDots mIndicatorDots;

    private View currentToggleRow;

    private HomeViewModel homeViewModel;
    private View root;

    private DashboardViewModel dashboardViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);
        root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        bindGUI();
        initData();

        return root;
    }
    private void bindGUI() {
    }
    private void initData(){
        app = (ApplicationMy) getActivity().getApplication();
        adapter = new PeopleAdapterDashboard(app);

        recyclerView = root.findViewById(R.id.rv_person_dashboard);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        adapter.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(View itemView, int position) {

    }

    @Override
    public void onToggleButton(View itemView, int position) {
        currentToggleRow = recyclerView.findViewHolderForAdapterPosition(position).itemView;
        // We reverse the toggle on click, because we have to wait for the PinLockActivity result
        ToggleButton tbToggleCheckInOut = (ToggleButton)currentToggleRow.findViewById(R.id.tbCheckInOut);
        tbToggleCheckInOut.setChecked(!tbToggleCheckInOut.isChecked());

        createNewPinLockDialog();
    }

    public void goAddPerson(MenuItem item){
        Intent intent = new Intent(getActivity().getBaseContext(), AddPersonActivity.class);
        startActivity(intent);
    }

    public void createNewPinLockDialog(){
        dialogBuilder = new AlertDialog.Builder(getActivity());
        final View pinLockPopupView = getLayoutInflater().inflate(R.layout.popup_pin_lock, null);

        mPinLockView = (PinLockView) pinLockPopupView.findViewById(R.id.pin_lock_view);
        mPinLockView.setPinLockListener(mPinLockListener);
        mIndicatorDots = (IndicatorDots) pinLockPopupView.findViewById(R.id.indicator_dots);
        mPinLockView.attachIndicatorDots(mIndicatorDots);
        ImageButton btExit = (ImageButton) pinLockPopupView.findViewById(R.id.btExit);
        btExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialogBuilder.setView(pinLockPopupView);
        dialog = dialogBuilder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }
    private PinLockListener mPinLockListener = new PinLockListener() {
        @Override
        public void onComplete(String pin) {
            Log.d(TAG, "Pin complete: " + pin);
            TextView tvName =  (TextView)currentToggleRow.findViewById(R.id.tv_name);
            String name = tvName.getText().toString();

            String currentPin = app.findPersonByName(name).getPin();
            if (pin.equals(currentPin)) {
                ToggleButton tbToggleCheckInOut = (ToggleButton)currentToggleRow.findViewById(R.id.tbCheckInOut);
                // We toggle the button back programmatically
                tbToggleCheckInOut.setChecked(!tbToggleCheckInOut.isChecked());

                LocalDateTime ltNow = LocalDateTime.now();
                boolean isCheckingIn = tbToggleCheckInOut.isChecked();

                // We add to firebase
                if(isCheckingIn){
                    app.checkInPerson(name, ltNow);
                }
                else{
                    app.checkOutPerson(name, ltNow);
                }

                // We add to adapter People array
                for (PeopleEditModel person : PeopleAdapterDashboard.peopleEditModelArrayList){
                    if(person.getName().equals(name)){
                        if(isCheckingIn){
                            person.addCheckIn(ltNow);
                        }
                        else{
                            person.addCheckOut(ltNow);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                dialog.dismiss();
            }
            else {
                dialog.dismiss();
            }
        }

        @Override
        public void onEmpty() {
            Log.d(TAG, "Pin empty");
        }

        @Override
        public void onPinChange(int pinLength, String intermediatePin) {
            Log.d(TAG, "Pin changed, new length " + pinLength + " with intermediate pin " + intermediatePin);
        }
    };

}