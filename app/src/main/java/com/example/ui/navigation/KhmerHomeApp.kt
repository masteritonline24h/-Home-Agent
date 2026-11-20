package com.example.ui.navigation

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.AddHomeWork
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.model.AgentRatingSummary
import com.example.data.model.InquiryStatus
import com.example.data.model.PropertyListing
import com.example.data.repository.RealEstateRepository
import com.example.data.repository.signOut
import com.example.ui.components.PropertyFormBottomSheet
import com.example.ui.screens.AgentHubScreen
import com.example.ui.screens.AgentProfileScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.InquiriesScreen
import com.example.ui.screens.PropertyDetailScreen
import com.example.ui.screens.SavedScreen
import com.example.ui.viewmodel.RealEstateViewModel
import com.example.ui.viewmodel.UiState

object AppRoutes {
    const val EXPLORE = "explore"
    const val SAVED = "saved"
    const val INQUIRIES = "inquiries"
    const val AGENT = "agent"
    const val DETAIL = "detail/{propertyId}"
    const val AGENT_PROFILE = "agent_profile/{agentId}?name={name}"
    const val CHAT = "chat/{chatId}"

    fun detail(propertyId: String): String = "detail/${Uri.encode(propertyId)}"
    fun agentProfile(agentId: String, agentName: String): String =
        "agent_profile/${Uri.encode(agentId)}?name=${Uri.encode(agentName)}"
    fun chat(chatId: String): String = "chat/${Uri.encode(chatId)}"
}

private data class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhmerHomeApp(currentUserId: String) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: RealEstateViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                RealEstateViewModel(
                    repository = RealEstateRepository(application),
                    currentUserId = currentUserId,
                )
            }
        }
    )

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val publicPropertiesState by viewModel.publicPropertiesState.collectAsStateWithLifecycle()
    val filteredPropertiesState by viewModel.filteredPropertiesState.collectAsStateWithLifecycle()
    val myPropertiesState by viewModel.myPropertiesState.collectAsStateWithLifecycle()
    val savedPropertiesState by viewModel.savedPropertiesState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val inquiriesState by viewModel.inquiriesState.collectAsStateWithLifecycle()
    val publicReviewsState by viewModel.publicReviewsState.collectAsStateWithLifecycle()
    val ratingSummaries by viewModel.agentRatingSummaries.collectAsStateWithLifecycle()
    val chatThreadsState by viewModel.chatThreadsState.collectAsStateWithLifecycle()
    val isActionBusy by viewModel.isActionBusy.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var showPropertyForm by remember { mutableStateOf(false) }
    var editingProperty by remember { mutableStateOf<PropertyListing?>(null) }

    val pendingLeadsCount = remember(inquiriesState, currentUserId) {
        val list = (inquiriesState as? UiState.Success)?.data.orEmpty()
        list.count { it.agentId == currentUserId && it.resolvedStatus() == InquiryStatus.PENDING }
    }

    val destinations = remember {
        listOf(
            TopLevelDestination(
                route = AppRoutes.EXPLORE,
                labelRes = R.string.nav_explore,
                selectedIcon = Icons.Filled.Search,
                unselectedIcon = Icons.Outlined.Search,
                testTag = "nav_explore",
            ),
            TopLevelDestination(
                route = AppRoutes.SAVED,
                labelRes = R.string.nav_saved,
                selectedIcon = Icons.Filled.Favorite,
                unselectedIcon = Icons.Outlined.FavoriteBorder,
                testTag = "nav_saved",
            ),
            TopLevelDestination(
                route = AppRoutes.INQUIRIES,
                labelRes = R.string.nav_inquiries,
                selectedIcon = Icons.AutoMirrored.Filled.Chat,
                unselectedIcon = Icons.AutoMirrored.Outlined.Chat,
                testTag = "nav_inquiries",
            ),
            TopLevelDestination(
                route = AppRoutes.AGENT,
                labelRes = R.string.nav_agent,
                selectedIcon = Icons.Filled.ManageAccounts,
                unselectedIcon = Icons.Outlined.ManageAccounts,
                testTag = "nav_agent",
            ),
        )
    }

    val isTopLevelRoute = destinations.any { dest ->
        currentDestination?.hierarchy?.any { it.route == dest.route } == true
    }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                if (isTopLevelRoute) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.HomeWork,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(id = R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                        ),
                    )
                }
            },
            bottomBar = {
                if (isTopLevelRoute && !useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        destinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    val iconVector = if (selected) dest.selectedIcon else dest.unselectedIcon
                                    if (dest.route == AppRoutes.INQUIRIES && pendingLeadsCount > 0) {
                                        BadgedBox(badge = { Badge { Text(pendingLeadsCount.toString()) } }) {
                                            Icon(iconVector, contentDescription = stringResource(dest.labelRes))
                                        }
                                    } else {
                                        Icon(iconVector, contentDescription = stringResource(dest.labelRes))
                                    }
                                },
                                label = { Text(stringResource(dest.labelRes), maxLines = 1) },
                                modifier = Modifier.testTag(dest.testTag),
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentRoute == AppRoutes.EXPLORE || currentRoute == AppRoutes.AGENT) {
                    FloatingActionButton(
                        onClick = {
                            editingProperty = null
                            showPropertyForm = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_post_property"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddHomeWork,
                            contentDescription = stringResource(id = R.string.post_property),
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (isTopLevelRoute && useNavigationRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        destinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    val iconVector = if (selected) dest.selectedIcon else dest.unselectedIcon
                                    if (dest.route == AppRoutes.INQUIRIES && pendingLeadsCount > 0) {
                                        BadgedBox(badge = { Badge { Text(pendingLeadsCount.toString()) } }) {
                                            Icon(iconVector, contentDescription = stringResource(dest.labelRes))
                                        }
                                    } else {
                                        Icon(iconVector, contentDescription = stringResource(dest.labelRes))
                                    }
                                },
                                label = { Text(stringResource(dest.labelRes), maxLines = 1) },
                                modifier = Modifier.testTag("${dest.testTag}_rail"),
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = AppRoutes.EXPLORE,
                    ) {
                        composable(AppRoutes.EXPLORE) {
                            val totalPublicCount = (publicPropertiesState as? UiState.Success)?.data?.size ?: 0
                            ExploreScreen(
                                propertiesState = filteredPropertiesState,
                                totalPublicCount = totalPublicCount,
                                favoriteIds = favoriteIds,
                                filterState = filterState,
                                isActionBusy = isActionBusy,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onListingTypeChange = viewModel::updateListingTypeFilter,
                                onCategoryChange = viewModel::updateCategoryFilter,
                                onApplyAdvancedFilters = viewModel::applyAdvancedFilters,
                                onClearFilters = viewModel::clearAllFilters,
                                onPropertyClick = { propId ->
                                    navController.navigate(AppRoutes.detail(propId))
                                },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onOpenCreateProperty = {
                                    editingProperty = null
                                    showPropertyForm = true
                                },
                                onSeedStarterPortfolio = viewModel::seedStarterPortfolio,
                            )
                        }

                        composable(AppRoutes.SAVED) {
                            SavedScreen(
                                savedState = savedPropertiesState,
                                onPropertyClick = { propId ->
                                    navController.navigate(AppRoutes.detail(propId))
                                },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onBrowseExplore = { navigateTopLevel(AppRoutes.EXPLORE) },
                            )
                        }

                        composable(AppRoutes.INQUIRIES) {
                            InquiriesScreen(
                                chatThreadsState = chatThreadsState,
                                inquiriesState = inquiriesState,
                                currentUserId = currentUserId,
                                isBusy = isActionBusy,
                                onOpenChat = { chatId ->
                                    navController.navigate(AppRoutes.chat(chatId))
                                },
                                onOpenProperty = { propId ->
                                    navController.navigate(AppRoutes.detail(propId))
                                },
                                onReplyInquiry = viewModel::replyToInquiry,
                                onCloseInquiry = viewModel::closeInquiryByBuyer,
                            )
                        }

                        composable(AppRoutes.AGENT) {
                            val myRatingSummary = ratingSummaries[currentUserId] ?: AgentRatingSummary()
                            AgentHubScreen(
                                userProfile = userProfile,
                                ratingSummary = myRatingSummary,
                                myPropertiesState = myPropertiesState,
                                pendingClientLeadsCount = pendingLeadsCount,
                                isBusy = isActionBusy,
                                onCreateProperty = {
                                    editingProperty = null
                                    showPropertyForm = true
                                },
                                onEditProperty = { listing ->
                                    editingProperty = listing
                                    showPropertyForm = true
                                },
                                onOpenProperty = { propId ->
                                    navController.navigate(AppRoutes.detail(propId))
                                },
                                onOpenMyReviews = {
                                    val name = userProfile?.displayName ?: "ភ្នាក់ងារ ផ្ទះខ្មែរ"
                                    navController.navigate(AppRoutes.agentProfile(currentUserId, name))
                                },
                                onUpdatePropertyStatus = viewModel::updatePropertyStatus,
                                onDeleteProperty = viewModel::deleteProperty,
                                onUpdateProfile = viewModel::updateAgentProfile,
                                onSignOut = {
                                    signOut(credentialManager = credentialManager, scope = scope)
                                },
                            )
                        }

                        composable(
                            route = AppRoutes.DETAIL,
                            arguments = listOf(navArgument("propertyId") { type = NavType.StringType }),
                        ) { backStackEntry ->
                            val propertyId = Uri.decode(backStackEntry.arguments?.getString("propertyId").orEmpty())
                            val property = viewModel.findPropertyById(propertyId)
                            val agentRating = property?.let { ratingSummaries[it.ownerId] } ?: AgentRatingSummary()

                            PropertyDetailScreen(
                                property = property,
                                isFavorited = propertyId in favoriteIds,
                                agentRatingSummary = agentRating,
                                userProfile = userProfile,
                                isBusy = isActionBusy,
                                onBack = { navController.popBackStack() },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onStartChat = { selectedProp ->
                                    viewModel.startPropertyChat(selectedProp) { chatId ->
                                        navController.navigate(AppRoutes.chat(chatId))
                                    }
                                },
                                onOpenAgentProfile = { agentId, agentName ->
                                    navController.navigate(AppRoutes.agentProfile(agentId, agentName))
                                },
                                onSubmitInquiry = viewModel::sendInquiry,
                            )
                        }

                        composable(
                            route = AppRoutes.AGENT_PROFILE,
                            arguments = listOf(
                                navArgument("agentId") { type = NavType.StringType },
                                navArgument("name") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                },
                            ),
                        ) { backStackEntry ->
                            val agentId = Uri.decode(backStackEntry.arguments?.getString("agentId").orEmpty())
                            val agentName = Uri.decode(backStackEntry.arguments?.getString("name").orEmpty())

                            val publicList = (publicPropertiesState as? UiState.Success)?.data.orEmpty()
                            val myList = (myPropertiesState as? UiState.Success)?.data.orEmpty()
                            val agentProperties = remember(publicList, myList, agentId) {
                                (publicList + myList)
                                    .distinctBy { it.id }
                                    .filter { it.ownerId == agentId }
                            }

                            AgentProfileScreen(
                                agentId = agentId,
                                agentName = agentName,
                                reviewsState = publicReviewsState,
                                agentProperties = agentProperties,
                                currentUserId = currentUserId,
                                isBusy = isActionBusy,
                                onBack = { navController.popBackStack() },
                                onSubmitReview = { rating, comment, onDone ->
                                    viewModel.submitAgentReview(
                                        agentId = agentId,
                                        agentName = agentName.ifBlank {
                                            agentProperties.firstOrNull()?.agentName ?: "ភ្នាក់ងារ"
                                        },
                                        rating = rating,
                                        comment = comment,
                                        onComplete = onDone,
                                    )
                                },
                                onDeleteReview = viewModel::deleteAgentReview,
                                onOpenProperty = { propId ->
                                    navController.navigate(AppRoutes.detail(propId))
                                },
                            )
                        }

                        composable(
                            route = AppRoutes.CHAT,
                            arguments = listOf(navArgument("chatId") { type = NavType.StringType }),
                        ) { backStackEntry ->
                            val chatId = Uri.decode(backStackEntry.arguments?.getString("chatId").orEmpty())
                            val threads = (chatThreadsState as? UiState.Success)?.data.orEmpty()
                            val thread = threads.firstOrNull { it.id == chatId }
                            val messagesFlow = remember(chatId, currentUserId) {
                                viewModel.observeMessagesForChat(chatId)
                            }
                            val messagesState by messagesFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)

                            ChatScreen(
                                chatThread = thread,
                                messagesState = messagesState,
                                currentUserId = currentUserId,
                                isBusy = isActionBusy,
                                onBack = { navController.popBackStack() },
                                onOpenProperty = { propId ->
                                    navController.navigate(AppRoutes.detail(propId))
                                },
                                onSendMessage = viewModel::sendChatMessage,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPropertyForm) {
        PropertyFormBottomSheet(
            editingProperty = editingProperty,
            userProfile = userProfile,
            isBusy = isActionBusy,
            onSave = { id, title, listingType, category, price, district, city, beds, baths, area, desc, imageKey, agentName, agency, hotline, telegram, visibility, status ->
                viewModel.saveProperty(
                    editingId = id,
                    title = title,
                    listingType = listingType,
                    category = category,
                    priceUsd = price,
                    district = district,
                    city = city,
                    bedrooms = beds,
                    bathrooms = baths,
                    areaSqm = area,
                    description = desc,
                    imageKey = imageKey,
                    agentName = agentName,
                    agentAgency = agency,
                    businessHotline = hotline,
                    telegramHandle = telegram,
                    visibility = visibility,
                    status = status,
                ) {
                    showPropertyForm = false
                    editingProperty = null
                }
            },
            onDismiss = {
                showPropertyForm = false
                editingProperty = null
            },
        )
    }
}
