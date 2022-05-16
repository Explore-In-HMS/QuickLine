package com.hms.quickline.presentation.home

import com.hms.quickline.core.base.BaseViewModel
import com.hms.quickline.domain.usecase.HomeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val homeUseCase: HomeUseCase) : BaseViewModel()