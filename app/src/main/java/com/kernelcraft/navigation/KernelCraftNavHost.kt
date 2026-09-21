package com.kernelcraft.navigation

import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kernelcraft.feature.detail.ComponentDetailScreen
import com.kernelcraft.feature.kernel.KernelStackExplorerScreen
import com.kernelcraft.feature.kernel.KernelStackScreen
import com.kernelcraft.feature.specs.FullSpecMatrixScreen
import com.kernelcraft.feature.splash.SplashScreen
import com.kernelcraft.feature.teardown.TeardownPlayerScreen
import com.kernelcraft.feature.teardown.TeardownViewModel
import com.kernelcraft.feature.transition.SiliconZoomTransitionScreen
import com.kernelcraft.feature.transition.SoCTransitionLayout

/**
 * Route destinations for KernelCraft single-activity navigation.
 */
object KernelCraftDestinations {
    const val SPLASH = "splash"
    const val TEARDOWN = "teardown"
    const val COMPONENT_DETAIL = "component_detail/{componentId}"
    const val FULL_SPECS = "full_specs"
    const val SILICON_TRANSITION = "silicon_transition"
    const val KERNEL_STACK = "kernel_stack"

    fun componentDetailRoute(componentId: String): String = "component_detail/$componentId"
}

/**
 * Top-level Jetpack Navigation Host for KernelCraft.
 *
 * Implements fluid crossfades and vertical transitions to maintain visual continuity
 * across the studio orange background (#DD5622) and OS stack transitions.
 */
@Composable
fun KernelCraftNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    sharedTeardownViewModel: TeardownViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = KernelCraftDestinations.SPLASH,
        modifier = modifier,
        enterTransition = { fadeIn(tween(300)) },
        exitTransition = { fadeOut(tween(300)) }
    ) {
        // Screen 0: Splash / Brand Reveal
        composable(
            route = KernelCraftDestinations.SPLASH,
            exitTransition = { fadeOut(tween(400)) }
        ) {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate(KernelCraftDestinations.TEARDOWN) {
                        popUpTo(KernelCraftDestinations.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // Screen 1: Interactive Teardown Viewport
        composable(
            route = KernelCraftDestinations.TEARDOWN,
            enterTransition = { fadeIn(tween(400)) },
            exitTransition = { fadeOut(tween(250)) }
        ) {
            val context = LocalContext.current
            TeardownPlayerScreen(
                viewModel = sharedTeardownViewModel,
                onNavigateBack = {
                    val activity = context as? Activity
                    if (!navController.popBackStack()) {
                        activity?.finish()
                    }
                },
                onOpenComponentDetail = { componentId ->
                    navController.navigate(KernelCraftDestinations.componentDetailRoute(componentId))
                },
                onOpenFullSpecs = {
                    navController.navigate(KernelCraftDestinations.FULL_SPECS)
                },
                onDiveInClick = {
                    // Dive-In handoff: initiates micro-zoom into physical SoC silicon die
                    navController.navigate(KernelCraftDestinations.SILICON_TRANSITION)
                }
            )
        }

        // Screen 2: Component Deep-Dive Detail
        composable(
            route = KernelCraftDestinations.COMPONENT_DETAIL,
            arguments = listOf(navArgument("componentId") { type = NavType.StringType }),
            enterTransition = {
                fadeIn(tween(300)) + slideInVertically(tween(350)) { it / 4 }
            },
            exitTransition = {
                fadeOut(tween(200)) + slideOutVertically(tween(350)) { it / 4 }
            }
        ) { backStackEntry ->
            val componentId = backStackEntry.arguments?.getString("componentId") ?: "soc_processor"
            ComponentDetailScreen(
                componentId = componentId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onDiveIntoKernel = {
                    navController.navigate(KernelCraftDestinations.SILICON_TRANSITION)
                }
            )
        }

        // Screen 3: Full Specification Matrix
        composable(
            route = KernelCraftDestinations.FULL_SPECS,
            enterTransition = {
                fadeIn(tween(300)) + slideInVertically(tween(350)) { it / 3 }
            },
            exitTransition = {
                fadeOut(tween(200)) + slideOutVertically(tween(300)) { it / 3 }
            }
        ) {
            FullSpecMatrixScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Screen 4: Shared Camera Micro-Zoom Transition ("Entering the Silicon")
        composable(
            route = KernelCraftDestinations.SILICON_TRANSITION,
            enterTransition = { fadeIn(tween(350)) },
            exitTransition = { fadeOut(tween(350)) }
        ) {
            SoCTransitionLayout(
                onZoomOutToTeardown = {
                    navController.popBackStack()
                }
            )
        }

        // Screen 5: OS & Kernel Architecture Explorer
        composable(
            route = KernelCraftDestinations.KERNEL_STACK,
            enterTransition = {
                fadeIn(tween(350)) + slideInVertically(tween(400)) { it / 4 }
            },
            exitTransition = {
                fadeOut(tween(250)) + slideOutVertically(tween(350)) { it / 4 }
            }
        ) {
            KernelStackExplorerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
