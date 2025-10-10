package com.example.evmobile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.evmobile.models.Booking
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import java.text.SimpleDateFormat
import java.util.*

class QRCodeActivity : AppCompatActivity() {
    
    // UI Components
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvStationName: TextView
    private lateinit var tvBookingDateTime: TextView
    private lateinit var tvBookingId: TextView
    private lateinit var ivQRCode: ImageView
    private lateinit var btnShareQR: Button
    private lateinit var btnSaveQR: Button
    
    // Data
    private var booking: Booking? = null
    private var qrCodeBitmap: Bitmap? = null
    
    companion object {
        const val EXTRA_BOOKING_ID = "booking_id"
        const val EXTRA_STATION_NAME = "station_name"
        const val EXTRA_BOOKING_DATE_TIME = "booking_date_time"
        const val EXTRA_QR_CODE = "qr_code"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code)
        
        initViews()
        setupToolbar()
        loadBookingData()
        generateQRCode()
        setupClickListeners()
    }
    
    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvStationName = findViewById(R.id.tvStationName)
        tvBookingDateTime = findViewById(R.id.tvBookingDateTime)
        tvBookingId = findViewById(R.id.tvBookingId)
        ivQRCode = findViewById(R.id.ivQRCode)
        btnShareQR = findViewById(R.id.btnShareQR)
        btnSaveQR = findViewById(R.id.btnSaveQR)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Booking QR Code"
        }
        
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun loadBookingData() {
        val bookingId = intent.getStringExtra(EXTRA_BOOKING_ID) ?: return
        val stationName = intent.getStringExtra(EXTRA_STATION_NAME) ?: "Unknown Station"
        val bookingDateTime = intent.getLongExtra(EXTRA_BOOKING_DATE_TIME, System.currentTimeMillis())
        val qrCode = intent.getStringExtra(EXTRA_QR_CODE) ?: ""
        
        // Update UI
        tvStationName.text = stationName
        tvBookingId.text = "Booking ID: $bookingId"
        
        // Format date and time
        val dateFormat = SimpleDateFormat("MMM dd, yyyy - h:mm a", Locale.getDefault())
        tvBookingDateTime.text = dateFormat.format(Date(bookingDateTime))
    }
    
    private fun generateQRCode() {
        val qrCodeData = intent.getStringExtra(EXTRA_QR_CODE) ?: return
        
        try {
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix: BitMatrix = qrCodeWriter.encode(qrCodeData, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            
            qrCodeBitmap = bitmap
            ivQRCode.setImageBitmap(bitmap)
            
        } catch (e: WriterException) {
            e.printStackTrace()
            showToast("Failed to generate QR code")
        }
    }
    
    private fun setupClickListeners() {
        btnShareQR.setOnClickListener {
            shareQRCode()
        }
        
        btnSaveQR.setOnClickListener {
            saveQRCode()
        }
    }
    
    private fun shareQRCode() {
        qrCodeBitmap?.let { bitmap ->
            // Create a simple text share for now (in real app, you'd share the image)
            val shareText = """
                My EV Charging Booking QR Code
                
                Station: ${tvStationName.text}
                Date & Time: ${tvBookingDateTime.text}
                Booking ID: ${intent.getStringExtra(EXTRA_BOOKING_ID)}
                
                QR Code Data: ${intent.getStringExtra(EXTRA_QR_CODE)}
            """.trimIndent()
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, "EV Charging Booking QR Code")
            }
            
            startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
        } ?: showToast("No QR code to share")
    }
    
    private fun saveQRCode() {
        qrCodeBitmap?.let {
            // In a real app, you would save to gallery
            // For now, just show a message
            showToast("QR code saved to gallery (Demo)")
        } ?: showToast("No QR code to save")
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}