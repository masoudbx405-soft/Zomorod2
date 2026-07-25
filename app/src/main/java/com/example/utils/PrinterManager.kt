package com.example.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

data class BluetoothPrinterDevice(
    val name: String,
    val address: String,
    val isConnected: Boolean = false
)

object PrinterManager {

    private val _connectedPrinter = MutableStateFlow<BluetoothPrinterDevice?>(null)
    val connectedPrinter: StateFlow<BluetoothPrinterDevice?> = _connectedPrinter

    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting

    private val _availablePrinters = MutableStateFlow<List<BluetoothPrinterDevice>>(emptyList())
    val availablePrinters: StateFlow<List<BluetoothPrinterDevice>> = _availablePrinters

    fun scanPrinters(context: Context) {
        val list = mutableListOf<BluetoothPrinterDevice>()
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            @SuppressLint("MissingPermission")
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
                pairedDevices?.forEach { device ->
                    @SuppressLint("MissingPermission")
                    list.add(BluetoothPrinterDevice(device.name ?: "Unknown Printer", device.address))
                }
            }
        } catch (e: Exception) {
            // Permission or BT disabled
        }

        // Add simulated thermal printers if list is empty or for demo
        if (list.none { it.name.contains("Thermal", true) || it.name.contains("POS", true) || it.name.contains("MTP", true) }) {
            list.add(BluetoothPrinterDevice("پرینتر حرارتی بلوتوثی BTP-58 (کارگاه)", "00:11:22:33:44:55"))
            list.add(BluetoothPrinterDevice("پرینتر سیار راننده (MTP-II)", "AA:BB:CC:DD:EE:FF"))
            list.add(BluetoothPrinterDevice("پرینتر قالیشویی زمرد (POS-80)", "12:34:56:78:9A:BC"))
        }

        _availablePrinters.value = list
    }

    suspend fun connectPrinter(device: BluetoothPrinterDevice): Boolean {
        return withContext(Dispatchers.IO) {
            delay(1000) // Simulating BT connection protocol handshakes
            _connectedPrinter.value = device.copy(isConnected = true)
            true
        }
    }

    fun disconnectPrinter() {
        _connectedPrinter.value = null
    }

    suspend fun printReceipt(
        title: String,
        orderId: String,
        customerName: String,
        customerPhone: String,
        address: String,
        carpetDetails: String,
        totalPrice: Long,
        discount: Long,
        finalPrice: Long,
        paymentStatus: String,
        rackCode: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            _isPrinting.value = true
            delay(1800) // Simulating Bluetooth stream writing and paper motor printing
            _isPrinting.value = false
            true
        }
    }

    fun buildEscPosThermalReceiptText(
        title: String,
        orderId: String,
        customerName: String,
        customerPhone: String,
        address: String,
        carpetItemsSummary: List<String>,
        totalPrice: Long,
        discount: Long,
        netPayable: Long,
        paymentMethod: String,
        rackCode: String
    ): String {
        val sb = StringBuilder()
        sb.append("===============================\n")
        sb.append("     *** قالیشویی زمرد ***\n")
        sb.append("    $title\n")
        sb.append("===============================\n")
        sb.append("شماره فاکتور: $orderId\n")
        sb.append("تاریخ و زمان: ${FarsiUtils.formatCurrentTimeFarsi()}\n")
        sb.append("نام مشتری: $customerName\n")
        sb.append("تلفن تماس: $customerPhone\n")
        sb.append("آدرس: $address\n")
        sb.append("-------------------------------\n")
        sb.append("اقلام سفارش (فرش‌ها):\n")
        carpetItemsSummary.forEachIndexed { index, item ->
            sb.append("${index + 1}. $item\n")
        }
        sb.append("-------------------------------\n")
        if (rackCode.isNotEmpty()) {
            sb.append("شماره قفسه انبار: $rackCode\n")
            sb.append("-------------------------------\n")
        }
        sb.append("مبلغ کل فرش‌ها: ${FarsiUtils.formatPrice(totalPrice)}\n")
        if (discount > 0) {
            sb.append("مبلغ تخفیف: ${FarsiUtils.formatPrice(discount)}\n")
        }
        sb.append("مبلغ قابل پرداخت: ${FarsiUtils.formatPrice(netPayable)}\n")
        sb.append("وضعیت تسویه: $paymentMethod\n")
        sb.append("===============================\n")
        sb.append(" امضاء و تایید تحویل‌گیرنده:\n\n\n")
        sb.append("...............................\n")
        sb.append("سامانه انحصاری قالیشویی زمرد\n")
        sb.append("===============================\n\n")
        return sb.toString()
    }
}
