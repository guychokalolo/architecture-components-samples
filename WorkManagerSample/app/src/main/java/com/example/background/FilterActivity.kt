/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.background

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.work.WorkInfo
import com.bumptech.glide.Glide
import com.example.background.databinding.ActivityFilterBinding

/** The [android.app.Activity] where the user picks filters to be applied on an image. */
class FilterActivity : AppCompatActivity() {

    private val viewModel: FilterViewModel by viewModels { FilterViewModelFactory(application) }
    private var outputImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityFilterBinding.inflate(layoutInflater).run {
            setContentView(root)
            bindViews(this)
            // Check to see if we have output.
            viewModel.outputStatus.observe(this@FilterActivity) { infoList ->
                onStateChange(infoList, this)
            }
        }
    }

    private fun bindViews(binding: ActivityFilterBinding) {
        with(binding) {
            val imageUri: Uri = Uri.parse(intent.getStringExtra(Constants.KEY_IMAGE_URI))
            Glide.with(this@FilterActivity).load(imageUri).into(imageView)

            // Disable upload to Imgur, unless the developer specifies their own clientId.
            val uploadEnabled = Constants.IMGUR_CLIENT_ID.isNotEmpty()
            upload.isEnabled = uploadEnabled

            apply.setOnClickListener {
                val applyWaterColor = filterWatercolor.isChecked
                val applyGrayScale = filterGrayscale.isChecked
                val applyBlur = filterBlur.isChecked
                val save = save.isChecked
                val upload = upload.isChecked

                val imageOperations = ImageOperations(
                    applicationContext, imageUri,
                    applyWaterColor, applyGrayScale, applyBlur,
                    save, upload
                )

                viewModel.apply(imageOperations)
            }

            output.setOnClickListener {
                if (outputImageUri != null) {
                    val viewOutput = Intent(Intent.ACTION_VIEW, outputImageUri)
                    if (viewOutput.resolveActivity(packageManager) != null) {
                        startActivity(viewOutput)
                    }
                }
            }
            cancel.setOnClickListener { viewModel.cancel() }
        }
    }

    private fun onStateChange(infoList: List<WorkInfo>, binding: ActivityFilterBinding) {
        if (infoList.isEmpty()) {
            return
        }
        // We only care about the one output status.
        // Every continuation has only one worker tagged TAG_OUTPUT
        val info = infoList[0]
        val finished = info.state.isFinished

        with(binding) {
            if (!finished) {
                progressBar.visibility = View.VISIBLE
                cancel.visibility = View.VISIBLE
                apply.visibility = View.GONE
                output.visibility = View.GONE
            } else {
                progressBar.visibility = View.GONE
                cancel.visibility = View.GONE
                apply.visibility = View.VISIBLE
            }
        }
        val outputData = info.outputData
        outputData.getString(Constants.KEY_IMAGE_URI)?.let {
            outputImageUri = Uri.parse(it)
            binding.output.visibility = View.VISIBLE
        }
    }

    companion object {

        /**
         * Creates a new intent which can be used to start [FilterActivity].
         *
         * @param context the application [Context].
         * @param imageUri the input image [Uri].
         * @return the instance of [Intent].
         */
        internal fun newIntent(context: Context, imageUri: Uri) =
            Intent(context, FilterActivity::class.java).putExtra(
                Constants.KEY_IMAGE_URI, imageUri.toString()
            )
    }
}
