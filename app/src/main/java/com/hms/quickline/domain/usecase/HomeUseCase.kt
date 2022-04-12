package com.hms.quickline.domain.usecase

import com.hms.quickline.data.repository.HomeRepository
import javax.inject.Inject

class HomeUseCase @Inject constructor(
    private val homeRepository: HomeRepository
) {

}