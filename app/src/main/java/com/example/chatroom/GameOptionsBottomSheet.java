package com.example.chatroom;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class GameOptionsBottomSheet extends BottomSheetDialogFragment {

    private ChatActivity chatActivity;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ChatActivity) {
            chatActivity = (ChatActivity) context;
        } else {
            dismiss();
        }
    }

    public GameOptionsBottomSheet() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_game_options, container, false);

        Button ticTacToeButton = view.findViewById(R.id.ticTacToeButton);
        Button fourInARowButton = view.findViewById(R.id.fourInARowButton);

        ticTacToeButton.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Requesting Tic Tac Toe!", Toast.LENGTH_SHORT).show();
            if (chatActivity != null) {
                chatActivity.initiateTicTacToeGame();
            }
            dismiss();
        });

        fourInARowButton.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Launching 4 In A Row!", Toast.LENGTH_SHORT).show();
            if (chatActivity != null) {
                chatActivity.initiateFourInARowGame();
            }
            dismiss();
        });

        return view;
    }
}