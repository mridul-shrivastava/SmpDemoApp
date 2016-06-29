package com.klouddata.push.pushdemoandroid.fragment.dialog;


import android.app.Activity;
import android.app.DialogFragment;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.klouddata.push.pushdemoandroid.R;

public class RegisterDialogFragment extends DialogFragment implements View.OnClickListener {
    private View mView;
    private Button mRegisterButton;
    private EditText mUserName, mPassword;
    private OnFragmentInteractionListener mListener;

    public RegisterDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.DialogStyle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_register_dialog, container, false);
        return mView;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        init();
    }


    private void init() {
        initViews();
        iniListener();
    }

    private void initViews() {
        mRegisterButton = (Button) mView.findViewById(R.id.register_button);
        mUserName = (EditText) mView.findViewById(R.id.user_name);
        mPassword = (EditText) mView.findViewById(R.id.password);
    }

    private void iniListener() {
        mRegisterButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.register_button:
                registerUser();
                break;
            default:
                break;
        }
    }

    private void registerUser() {
        String user = mUserName.getText().toString().trim();
        String password = mPassword.getText().toString().trim();
        mListener.onRegisterPressed(user, password);
        dismiss();
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onRegisterPressed(String userName, String password);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
