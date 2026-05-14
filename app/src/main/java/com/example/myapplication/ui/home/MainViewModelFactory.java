package com.example.myapplication.ui.home;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import android.content.SharedPreferences;

import com.example.myapplication.data.NewsRepository;

public class MainViewModelFactory implements ViewModelProvider.Factory {

    private final NewsRepository repository;
    private final SharedPreferences prefs;

    public MainViewModelFactory(NewsRepository repository, SharedPreferences prefs) {
        this.repository = repository;
        this.prefs = prefs;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MainViewModel.class)) {
            MainViewModel vm = new MainViewModel();
            vm.init(repository, prefs);
            return (T) vm;
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
