@file:Suppress("UnusedImport")

package com.example.smartparking

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.foundation.clickable
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.res.painterResource

// ----------------------------
// Data model (sesuaikan bila sdh ada di project Anda)
// ----------------------------
data class Slot(
    val id: String,
    val xPct: Float,  // 0f..1f dari kiri
    val yPct: Float,  // 0f..1f dari atas
    val wPct: Float,  // 0f..1f dari lebar container
    val hPct: Float,  // 0f..1f dari tinggi container
    val accessible: Boolean = false,
    val occupied: Boolean = false
)

data class Lot(
    val name: String,
    @DrawableRes val imageRes: Int,
    val free: Int,
    val used: Int,
    val slots: List<Slot>
)

// ----------------------------
// SAMPLE PAGE
// ----------------------------
@Composable
fun LiveParkingPage_DtetiPreview(
    // anggapan: ini slot parkir DTETI FT UGM
    @DrawableRes mapRes: Int,
    initialLot: Lot = sampleLot(mapRes)
) {
    // Header TETAP (tidak tergantung state lot)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ===== Header (tidak ikut refresh) =====
        Text(
            "Live Parking",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            "Kantong Parkir DTETI • Fakultas Teknik UGM",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))

        // ===== Body: hanya bagian ini yang akan recompose saat refresh =====
        var lot by remember { mutableStateOf(initialLot) }
        LotCard(
            lot = lot,
            onRefresh = {
                // simulasi: toggle beberapa slot untuk mendemokan "refresh parsial"
                val toggled = lot.slots.mapIndexed { idx, s ->
                    if (idx % 3 == 0) s.copy(occupied = !s.occupied) else s
                }
                val used = toggled.count { it.occupied }
                val free = toggled.size - used
                lot = lot.copy(slots = toggled, used = used, free = free)
            }
        )
    }
}

// ----------------------------
// Kartu lot dengan overlay presisi + tombol refresh di bawah peta
// ----------------------------
@Composable
private fun LotCard(
    lot: Lot,
    onRefresh: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            // Nama lokasi + ringkasan jumlah
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    lot.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                AssistChip(
                    onClick = { /* reserved for future filter */ },
                    label = { Text("${lot.free} kosong • ${lot.used} terpakai") }
                )
            }

            Spacer(Modifier.height(10.dp))

            // ===== Peta + overlay slot proporsional presisi =====
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                val density = LocalDensity.current
                val boxWidthPx = with(density) { maxWidth.toPx() }
                val boxHeightPx = with(density) { maxHeight.toPx() }

                Image(
                    painter = painterResource(lot.imageRes),
                    contentDescription = lot.name,
                    modifier = Modifier
                        .fillMaxSize(),
                    contentScale = ContentScale.FillBounds // overlay mengikuti persis container
                )

                // Gambar overlay PAKAI PIXEL → akurat & tidak loncat-loncatan rounding
                lot.slots.forEach { s ->
                    // jaga domain persen
                    val xPct = s.xPct.coerceIn(0f, 1f)
                    val yPct = s.yPct.coerceIn(0f, 1f)
                    val wPct = s.wPct.coerceIn(0f, 1f)
                    val hPct = s.hPct.coerceIn(0f, 1f)

                    val wPx = (boxWidthPx * wPct).coerceAtLeast(1f)
                    val hPx = (boxHeightPx * hPct).coerceAtLeast(1f)

                    // pastikan tidak keluar kanan/bawah
                    val maxX = (boxWidthPx - wPx).coerceAtLeast(0f)
                    val maxY = (boxHeightPx - hPx).coerceAtLeast(0f)

                    val xPx = (boxWidthPx * xPct).coerceIn(0f, maxX)
                    val yPx = (boxHeightPx * yPct).coerceIn(0f, maxY)

                    val wDp = with(density) { wPx.toDp() }
                    val hDp = with(density) { hPx.toDp() }

                    val baseColor =
                        if (s.accessible) Color(0xFF2F65F5) // biru: aksesibilitas
                        else if (s.occupied) Color(0xFFD93636) // merah: terpakai
                        else Color(0xFF18B46E) // hijau: kosong

                    Box(
                        modifier = Modifier
                            .absoluteOffset {
                                IntOffset(xPx.roundToInt(), yPx.roundToInt())
                            }
                            .size(width = wDp, height = hDp)
                            .background(
                                baseColor.copy(alpha = if (s.accessible) 0.90f else 0.82f),
                                RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (s.accessible) "♿ ${s.id}" else s.id,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }

            // ===== Tombol Refresh di bawah peta =====
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onRefresh,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2F65F5),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Refresh")
            }
        }
    }
}

// ----------------------------
// SAMPLE DATA (anggap DTETI FT UGM). Ubah sesuai sumber Anda.
// ----------------------------
fun sampleLot(@DrawableRes mapRes: Int): Lot {
    val slots = listOf(
        Slot("S1", 0.05f, 0.62f, 0.10f, 0.27f, occupied = true),
        Slot("S2", 0.17f, 0.62f, 0.10f, 0.27f),
        Slot("S3", 0.29f, 0.62f, 0.10f, 0.27f, occupied = true),
        Slot("S4", 0.41f, 0.62f, 0.10f, 0.27f),
        Slot("S5", 0.53f, 0.62f, 0.10f, 0.27f),
        Slot("S6", 0.65f, 0.62f, 0.10f, 0.27f, accessible = true),
        Slot("S7", 0.77f, 0.62f, 0.10f, 0.27f)
    )
    val used = slots.count { it.occupied }
    val free = slots.size - used
    return Lot(
        name = "DTETI FT UGM",
        imageRes = mapRes,
        free = free,
        used = used,
        slots = slots
    )
}

