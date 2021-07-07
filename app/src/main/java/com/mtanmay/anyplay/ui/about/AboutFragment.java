package com.mtanmay.anyplay.ui.about;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mtanmay.anyplay.R;

public class AboutFragment extends Fragment {

    ImageView github, linkedin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        github = view.findViewById(R.id.github);
        linkedin = view.findViewById(R.id.linkedin);

        github.setOnClickListener(v -> {
            String git = "https://github.com/MTanmay01/any-play";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(git));
            startActivity(i);

        });

        linkedin.setOnClickListener(v -> {
            String link = "https://www.linkedin.com/in/tanmay-maiti-876b2119b/";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(link));
            startActivity(i);
        });

    }
}