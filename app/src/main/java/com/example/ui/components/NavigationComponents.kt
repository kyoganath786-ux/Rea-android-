package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag

data class NavDestination(
  val route: String,
  val label: String,
  val icon: ImageVector,
  val testTag: String
)

val NAV_ITEMS = listOf(
  NavDestination("dashboard", "Dashboard", Icons.Default.Dashboard, "nav_dashboard"),
  NavDestination("chat", "Chat", Icons.AutoMirrored.Filled.Chat, "nav_chat"),
  NavDestination("analysis", "Analysis", Icons.Default.FindInPage, "nav_analysis"),
  NavDestination("termux", "Termux", Icons.Default.Terminal, "nav_termux"),
  NavDestination("tools", "Tools", Icons.Default.Build, "nav_tools"),
  NavDestination("settings", "Settings", Icons.Default.Settings, "nav_settings")
)

@Composable
fun NickAiBottomBar(
  currentRoute: String,
  onNavigate: (String) -> Unit
) {
  NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("nick_ai_bottom_nav")
  ) {
    NAV_ITEMS.forEach { item ->
      val selected = currentRoute == item.route
      NavigationBarItem(
        selected = selected,
        onClick = { onNavigate(item.route) },
        icon = { Icon(item.icon, contentDescription = item.label) },
        label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = MaterialTheme.colorScheme.primary,
          selectedTextColor = MaterialTheme.colorScheme.primary,
          indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
          unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
          unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.testTag(item.testTag)
      )
    }
  }
}

@Composable
fun NickAiNavigationRail(
  currentRoute: String,
  onNavigate: (String) -> Unit
) {
  NavigationRail(
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("nick_ai_nav_rail")
  ) {
    NAV_ITEMS.forEach { item ->
      val selected = currentRoute == item.route
      NavigationRailItem(
        selected = selected,
        onClick = { onNavigate(item.route) },
        icon = { Icon(item.icon, contentDescription = item.label) },
        label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
        colors = NavigationRailItemDefaults.colors(
          selectedIconColor = MaterialTheme.colorScheme.primary,
          selectedTextColor = MaterialTheme.colorScheme.primary,
          indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        modifier = Modifier.testTag(item.testTag)
      )
    }
  }
}
