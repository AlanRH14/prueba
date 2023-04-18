package mx.com.superapp.gssavisualcomponents.uicatalog.compose.widget.main

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.appsamurai.storyly.*
import com.appsamurai.storyly.analytics.StorylyEvent
import com.appsamurai.storyly.styling.StoryGroupTextStyling
import com.google.gson.Gson
import mx.com.superapp.gssafunctionalutilities.GSFUTools
import mx.com.superapp.gssafunctionalutilities.extension.d
import mx.com.superapp.gssainterceptor.redux.ApplicationActionDispatcher
import mx.com.superapp.gssainterceptor.redux.navigation.entrypoints.IntentEntrypointStrategy
import mx.com.superapp.gssavisualcomponents.R
import mx.com.superapp.gssavisualcomponents.gsvuutils.ConstantsEvent
import mx.com.superapp.gssavisualcomponents.uicatalog.compose.widget.models.GSVCStorylyModel
import mx.com.superapp.gssavisualcomponents.uicatalog.compose.widget.models.parsing.GSVCElementModel
import mx.com.superapp.gssavisualcomponents.uicatalog.compose.widget.support.gsvccoahmark.data.GSVCCoachMarkModel
import mx.com.superapp.gssavisualcomponents.uicatalog.compose.widget.support.gsvccoahmark.data.GSVCCoachMarkText
import org.apache.commons.lang3.CharEncoding.UTF_8
import java.net.URLDecoder


@Composable
fun GSVCStorylyCard(
    model: GSVCElementModel,
    modifier: Modifier = Modifier,
    intent: Intent = Intent(),
    onTaggedStory: (GSVCStorylyModel) -> Unit,
    coachMarkModel: GSVCCoachMarkModel? = null,
    onCoachMark: ((GSVCCoachMarkModel) -> Unit)? = null
) {
    val isNotDataStoryly = remember { mutableStateOf(false) }
    GSFUTools.Logs.d("Init:: GSVCStorylyCard")
    if (isNotDataStoryly.value.not()) {
        Column(
            modifier = modifier
                .onGloballyPositioned {
                    if (coachMarkModel != null) {
                        onCoachMark?.invoke(
                            GSVCCoachMarkModel(
                                coordinates = it,
                                gsvcCoachMarkText = GSVCCoachMarkText(
                                    focusWorlds = coachMarkModel.gsvcCoachMarkText?.focusWorlds
                                        ?: emptyList(),
                                    title = coachMarkModel.gsvcCoachMarkText?.title ?: ""
                                )
                            )
                        )
                    }
                }
                .fillMaxWidth()
        ) {
            GSVCStorylyCard(model, intent, onTaggedStory, isNotDataStoryly)
        }
    }

}

@Composable
fun GSVCStorylyCard(
    model: GSVCElementModel,
    intent: Intent = Intent(),
    onTaggedStory: (GSVCStorylyModel) -> Unit,
    isNotDataStoryly: MutableState<Boolean>
) {
    val context = LocalContext.current
    val origin = model.name.lowercase()
    val colorRange = if (model.style != 0) model.style else R.color.gsvc_aux_D_100
    val tokenStory = remember { mutableStateOf(model.data) }
    val deepLinkMsg = remember {
        mutableStateOf("")
    }
    val uri: Uri? = intent.data
    AndroidView(
        modifier = Modifier
            .wrapContentWidth(Alignment.CenterHorizontally)
            .fillMaxSize()
            .horizontalScroll(rememberScrollState(), true),
        factory = { ctx ->
            StorylyView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setStoryInteractiveTextTypeface(resources.getFont(R.font.poppins_regular))
                    setStoryItemTextTypeface(resources.getFont(R.font.poppins_regular))
                    setStoryGroupTextStyling(
                        StoryGroupTextStyling(
                            maxLines = 1,
                            typeface = resources.getFont(R.font.poppins_regular)
                        )
                    )
                } else {
                    setStoryGroupTextStyling(StoryGroupTextStyling(maxLines = 1))
                }
                setStoryGroupIconBackgroundColor(Color.parseColor("#FFFFFFFF"))
                setStoryGroupSize(StoryGroupSize.Small)
                setStoryItemProgressBarColor(
                    arrayOf(
                        context.getColor(R.color.gsvc_white),
                        context.getColor(colorRange)
                    )
                )
                setStoryGroupIconBorderColorNotSeen(
                    arrayOf(
                        context.getColor(colorRange),
                        context.getColor(colorRange)
                    )
                )
            }
        },
        update = { view ->
            view.visibility = View.GONE
            if (uri != null) {
                val parameters: List<String> = uri.pathSegments
                val param = parameters[parameters.size - 1]
                deepLinkMsg.value = param
                view.openStory(uri)
            }
            if (tokenStory.value != null && tokenStory.value.isNotEmpty()) {
                view.storylyInit = StorylyInit(tokenStory.value, customParameter = model.sicu)
                view.storylyListener = object : StorylyListener {
                    var initialLoad = true

                    var storylyLoaded = false

                    override fun storylyLoaded(
                        storylyView: StorylyView,
                        storyGroupList: List<StoryGroup>,
                        dataSource: StorylyDataSource
                    ) {
                        if (initialLoad && storyGroupList.isNotEmpty()) {
                            initialLoad = false
                            storylyView.refresh()
                            storylyView.visibility = View.VISIBLE
                        }

                        if (storyGroupList.isNotEmpty()) {
                            storylyLoaded = true
                            isNotDataStoryly.value = false
                        } else {
                            isNotDataStoryly.value = true
                            storylyLoaded = false
                        }
                        super.storylyLoaded(storylyView, storyGroupList, dataSource)
                    }

                    override fun storylyLoadFailed(
                        storylyView: StorylyView,
                        errorMessage: String
                    ) {
                        if (!storylyLoaded) {
                            storylyView.visibility = View.GONE
                        }
                        onTaggedStory.invoke(
                            GSVCStorylyModel(
                                eventName = "ui_interaction",
                                origin = origin
                            )
                        )
                        super.storylyLoadFailed(storylyView, errorMessage)
                    }

                    override fun storylyStoryShowFailed(
                        storylyView: StorylyView,
                        errorMessage: String
                    ) {
                        try {
                            onTaggedStory.invoke(
                                GSVCStorylyModel(
                                    eventName = "ui_interaction",
                                    origin = origin
                                )
                            )
                            super.storylyStoryShowFailed(storylyView, errorMessage)
                        } catch (exception: Exception) {
                            println("storyly ${exception.message}")
                        }
                    }


                    override fun storylyActionClicked(
                        storylyView: StorylyView,
                        story: Story
                    ) {
                        try {
                            story.media.let { storyMedia ->
                                val url = storyMedia.actionUrl ?: return
                                if (url.contains("https://")) {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse(url)
                                        }
                                    )
                                } else {
                                    storylyView.dismiss(1).apply {
                                        goToFlow(url)
                                    }

                                }
                            }
                        } catch (exception: Exception) {
                            GSFUTools.Logs.d("storyly ${exception.message}")
                        }
                    }


                    override fun storylyEvent(
                        storylyView: StorylyView,
                        event: StorylyEvent,
                        storyGroup: StoryGroup?,
                        story: Story?,
                        storyComponent: StoryComponent?
                    ) {
                        super.storylyEvent(
                            storylyView,
                            event,
                            storyGroup,
                            story,
                            storyComponent
                        )
                        when (event) {
                            StorylyEvent.StoryGroupClosed -> {
                                onTaggedStory.invoke(
                                    GSVCStorylyModel(
                                        uniqueId = story?.uniqueId ?: String(),
                                        title = story?.title ?: String(),
                                        currentTime = story?.currentTime ?: 0L,
                                        element = "cerrar",
                                        eventName = "ui_interaction",
                                        origin = origin
                                    )
                                )
                            }
                            StorylyEvent.StoryGroupOpened -> {
                                onTaggedStory.invoke(
                                    GSVCStorylyModel(
                                        uniqueId = story?.uniqueId ?: String(),
                                        title = story?.title ?: String(),
                                        currentTime = story?.currentTime ?: 0L,
                                        eventName = "pageview",
                                        answer = ConstantsEvent.PARAM_OPEN,
                                        origin = origin
                                    )
                                )
                            }
                            StorylyEvent.StoryShared -> {
                                onTaggedStory.invoke(
                                    GSVCStorylyModel(
                                        uniqueId = story?.uniqueId ?: String(),
                                        title = story?.title ?: String(),
                                        currentTime = story?.currentTime ?: 0L,
                                        eventName = "ui_interaction",
                                        element = "compartir",
                                        origin = origin
                                    )
                                )
                            }
                            StorylyEvent.StoryGroupPreviousSwiped -> {
                                onTaggedStory.invoke(
                                    GSVCStorylyModel(
                                        uniqueId = story?.uniqueId ?: String(),
                                        title = story?.title ?: String(),
                                        currentTime = story?.currentTime ?: 0L,
                                        eventName = "ui_interaction",
                                        element = ConstantsEvent.PARAM_PREVIOUS_SWIPE,
                                        origin = origin
                                    )
                                )
                            }
                            StorylyEvent.StoryGroupNextSwiped -> {
                                onTaggedStory.invoke(
                                    GSVCStorylyModel(
                                        uniqueId = story?.uniqueId ?: String(),
                                        title = story?.title ?: String(),
                                        currentTime = story?.currentTime ?: 0L,
                                        eventName = "ui_interaction",
                                        element = ConstantsEvent.PARAM_NEXT_SWIPE,
                                        origin = origin
                                    )
                                )
                            }
                            StorylyEvent.StoryPreviousClicked -> {
                                onTaggedStory.invoke(
                                    GSVCStorylyModel(
                                        uniqueId = story?.uniqueId ?: String(),
                                        title = story?.title ?: String(),
                                        currentTime = story?.currentTime ?: 0L,
                                        eventName = "ui_interaction",
                                        element = ConstantsEvent.PARAM_PREVIOUS_CLICKED,
                                        origin = origin
                                    )
                                )
                            }
                            StorylyEvent.StoryNextClicked -> {
                                onTaggedStory.invoke(
                                    GSVCStorylyModel(
                                        uniqueId = story?.uniqueId ?: String(),
                                        title = story?.title ?: String(),
                                        currentTime = story?.currentTime ?: 0L,
                                        eventName = "ui_interaction",
                                        element = ConstantsEvent.PARAM_NEXT_CLICKED,
                                        origin = origin
                                    )
                                )
                            }
                            StorylyEvent.StoryPaused -> {
                                onTaggedStory.invoke(
                                    GSVCStorylyModel(
                                        uniqueId = story?.uniqueId ?: String(),
                                        title = story?.title ?: String(),
                                        currentTime = story?.currentTime ?: 0L,
                                        eventName = "ui_interaction",
                                        answer = ConstantsEvent.PARAM_STORY_PAUSED,
                                        origin = origin
                                    )
                                )
                            }
                            StorylyEvent.StoryResumed -> {
                                onTaggedStory.invoke(
                                    GSVCStorylyModel(
                                        uniqueId = story?.uniqueId ?: String(),
                                        title = story?.title ?: String(),
                                        currentTime = story?.currentTime ?: 0L,
                                        eventName = "ui_interaction",
                                        answer = ConstantsEvent.PARAM_STORY_RESUMED,
                                        origin = origin
                                    )
                                )
                            }
                            StorylyEvent.StoryGroupDeepLinkOpened,
                            StorylyEvent.StoryCTAClicked -> {
                                onTaggedStory.invoke(
                                    GSVCStorylyModel(
                                        uniqueId = story?.uniqueId ?: String(),
                                        title = story?.title ?: String(),
                                        currentTime = story?.currentTime ?: 0L,
                                        eventName = "ui_interaction",
                                        element = "cta",
                                        origin = origin
                                    )
                                )
                            }
                            StorylyEvent.StoryCompleted -> {
                                onTaggedStory.invoke(
                                    GSVCStorylyModel(
                                        uniqueId = story?.uniqueId ?: String(),
                                        title = story?.title ?: String(),
                                        currentTime = story?.currentTime ?: 0L,
                                        eventName = "ui_interaction",
                                        element = "historia_vista",
                                        origin = origin
                                    )
                                )
                            }
                            StorylyEvent.StoryCommentSent -> {
                                onTaggedStory.invoke(
                                    GSVCStorylyModel(
                                        uniqueId = story?.uniqueId ?: String(),
                                        title = story?.title ?: String(),
                                        currentTime = story?.currentTime ?: 0L,
                                        eventName = "ui_interaction",
                                        idType = "comentarios",
                                        origin = origin
                                    )
                                )
                            }
                            StorylyEvent.StoryEmojiClicked -> {
                                onTaggedStory.invoke(
                                    GSVCStorylyModel(
                                        uniqueId = story?.uniqueId ?: String(),
                                        title = story?.title ?: String(),
                                        currentTime = story?.currentTime ?: 0L,
                                        eventName = "ui_interaction",
                                        idType = "emoji_clickeando",
                                        origin = origin
                                    )
                                )
                            }
                            StorylyEvent.StoryPollAnswered -> {
                                onTaggedStory.invoke(
                                    GSVCStorylyModel(
                                        uniqueId = story?.uniqueId ?: String(),
                                        title = story?.title ?: String(),
                                        currentTime = story?.currentTime ?: 0L,
                                        eventName = "ui_interaction",
                                        idType = "encuesta_contestada",
                                        origin = origin
                                    )
                                )
                            }
                            StorylyEvent.StoryRated -> {
                                onTaggedStory.invoke(
                                    GSVCStorylyModel(
                                        uniqueId = story?.uniqueId ?: String(),
                                        title = story?.title ?: String(),
                                        currentTime = story?.currentTime ?: 0L,
                                        eventName = "ui_interaction",
                                        idType = "reacciones",
                                        origin = origin
                                    )
                                )
                            }

                            else -> {}
                        }
                    }

                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewGameCard() {
    GSVCStorylyCard(
        GSVCElementModel(
            image = "background_entertainment_games",
            title = "Cake Slice Ninja",
            subTitle = "Arcade",
        ),
        intent = Intent(),
        onTaggedStory = {}
    ) {}
}


private fun goToFlow(url: String) {
    val jsonData = getJsonUrl(url)
    val intent = Intent(Intent.ACTION_VIEW)
    intent.putExtra("KEY_PARAMS", jsonData)
    intent.let { intent ->
        ApplicationActionDispatcher.dispatch(
            IntentEntrypointStrategy(intent).getAction()
        )
    }
}

private fun getJsonUrl(url: String): String? {
    val values = Uri.parse(url)
    val query = values.query
    val data: MutableMap<String, String> = HashMap()
    for (param in query?.split("&")?.toTypedArray()!!) {
        val params = param.split("=").toTypedArray()
        val paramName = URLDecoder.decode(params[0], UTF_8)
        var value = ""
        if (params.size == 2) {
            value = URLDecoder.decode(params[1], UTF_8)
        }
        data[paramName] = value
    }
    return Gson().toJson(data)
}
