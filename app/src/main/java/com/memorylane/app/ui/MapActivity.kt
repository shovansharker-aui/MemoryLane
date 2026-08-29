package com.memorylane.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.memorylane.app.R
import com.memorylane.app.data.AppDatabase
import com.memorylane.app.data.MediaScanner
import com.memorylane.app.data.PhotoMetadataReader
import com.memorylane.app.databinding.ActivityMapBinding
import com.memorylane.app.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.io.File

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // osmdroid needs its cache location + a user agent configured
        // before the MapView is created — otherwise tile loading fails.
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
        }

        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val projectName = intent.getStringExtra(GalleryActivity.EXTRA_PROJECT_NAME) ?: "Map"
        val folderUri = intent.getStringExtra(GalleryActivity.EXTRA_FOLDER_URI)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.title = "$projectName · Map"
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)

        if (folderUri == null) {
            binding.progressBar.visibility = android.view.View.GONE
            binding.textEmpty.visibility = android.view.View.VISIBLE
            return
        }

        loadLocations(folderUri)
    }

    private fun loadLocations(folderUri: String) {
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                MediaScanner.scan(this@MapActivity, folderUri).filter { !it.isVideo }
            }

            val dao = AppDatabase.getDatabase(this@MapActivity).photoAnalysisDao()
            val cached = withContext(Dispatchers.IO) {
                dao.getForUris(items.map { it.uri.toString() })
            }.associateBy { it.uri }

            val locatedItems = withContext(Dispatchers.Default) {
                items.mapNotNull { item ->
                    val cachedEntry = cached[item.uri.toString()]
                    val lat = cachedEntry?.latitude
                    val lon = cachedEntry?.longitude

                    if (lat != null && lon != null) {
                        Triple(item, lat, lon)
                    } else {
                        val meta = PhotoMetadataReader.read(this@MapActivity, item.uri)
                        if (meta.latitude != null && meta.longitude != null) {
                            Triple(item, meta.latitude, meta.longitude)
                        } else {
                            null
                        }
                    }
                }
            }

            binding.progressBar.visibility = android.view.View.GONE

            if (locatedItems.isEmpty()) {
                binding.textEmpty.visibility = android.view.View.VISIBLE
                return@launch
            }

            plotMarkers(locatedItems)
        }
    }

    private fun plotMarkers(locatedItems: List<Triple<MediaItem, Double, Double>>) {
        val points = mutableListOf<GeoPoint>()

        for ((item, lat, lon) in locatedItems) {
            val point = GeoPoint(lat, lon)
            points.add(point)

            val marker = Marker(binding.mapView)
            marker.position = point
            marker.title = item.name
            marker.setOnMarkerClickListener { _, _ ->
                val intent = Intent(this, MediaViewerActivity::class.java).apply {
                    putExtra(MediaViewerActivity.EXTRA_URI, item.uri.toString())
                    putExtra(MediaViewerActivity.EXTRA_IS_VIDEO, false)
                }
                startActivity(intent)
                true
            }
            binding.mapView.overlays.add(marker)
        }

        // Center the map on the average location of all pins, at a
        // reasonable zoom level to see the whole spread.
        val avgLat = points.map { it.latitude }.average()
        val avgLon = points.map { it.longitude }.average()
        binding.mapView.controller.setZoom(10.0)
        binding.mapView.controller.setCenter(GeoPoint(avgLat, avgLon))
        binding.mapView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }
}
