package dev.iconfy.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.iconfy.sample.icons.Iconfy

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    IconGallery()
                }
            }
        }
    }
}

@Composable
private fun IconGallery() {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Iconfy — Iconify icons in Compose", style = MaterialTheme.typography.titleMedium)
        IconRow("mdi:home", Iconfy.Mdi.Home)
        IconRow("tabler:user", Iconfy.Tabler.User)
        IconRow("lucide:heart", Iconfy.Lucide.Heart)
        IconRow("lucide:star", Iconfy.Lucide.Star)
        IconRow("mdi:github", Iconfy.Mdi.Github)
        IconRow("material-symbols:settings", Iconfy.MaterialSymbols.Settings)
        IconRow("ph:gear-six-fill", Iconfy.Ph.GearSixFill)
        Text("category(\"Dashboard\")", style = MaterialTheme.typography.titleSmall)
        IconRow("Dashboard.Nav.Main (renamed prefix + icon)", Iconfy.Dashboard.Nav.Main)
        IconRow("Dashboard.Tabler.Settings", Iconfy.Dashboard.Tabler.Settings)
    }
}

@Composable
private fun IconRow(label: String, icon: ImageVector) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(label)
    }
}
