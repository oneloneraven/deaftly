package com.ravendarque.deaftly.storage

import com.ravendarque.deaftly.core.settings.ShowTextSize

internal fun ShowTextSizeDto.toDomain(): ShowTextSize =
    when (this) {
        ShowTextSizeDto.Small -> ShowTextSize.Small
        ShowTextSizeDto.Medium -> ShowTextSize.Medium
        ShowTextSizeDto.Large -> ShowTextSize.Large
    }

internal fun ShowTextSize.toDto(): ShowTextSizeDto =
    when (this) {
        ShowTextSize.Small -> ShowTextSizeDto.Small
        ShowTextSize.Medium -> ShowTextSizeDto.Medium
        ShowTextSize.Large -> ShowTextSizeDto.Large
    }
