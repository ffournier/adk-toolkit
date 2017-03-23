package me.palazzetti.adktoolkit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import me.palazzetti.adktoolkit.response.AdkMessage;

/**
 * Allows to manage your ADK Usb Interface. It exposes
 * methods to open/close connected accessories and to read/write
 * from/to it.
 */

public class AdkManager implements IAdkManager {
    private static final String LOG_TAG = "AdkManager";
    private static final int BUFFER_SIZE = 255;

    private UsbManager mUsbManager;
    private UsbAccessory mUsbAccessory;
    private ParcelFileDescriptor mParcelFileDescriptor;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    private IntentFilter mDetachedFilter;
    private BroadcastReceiver mUsbReceiver;

    private int mByteRead = 0;

    public AdkManager(UsbManager mUsbManager) {
        // Store Android UsbManager reference
        this.mUsbManager = mUsbManager;
        attachFilters();
        attachUsbReceiver();
    }

    public AdkManager(Context ctx) {
        // Store Android UsbManager reference
        this.mUsbManager = (UsbManager) ctx.getSystemService(Context.USB_SERVICE);
        attachFilters();
        attachUsbReceiver();
    }

    private void attachFilters() {
        // Filter for detached events
        mDetachedFilter = new IntentFilter();
        mDetachedFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
    }

    private void attachUsbReceiver() {
        // Broadcast Receiver
        mUsbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (action.equals(UsbManager.ACTION_USB_ACCESSORY_DETACHED)) {
                    UsbAccessory usbAccessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                    if (usbAccessory != null && usbAccessory.equals(mUsbAccessory)) {
                        closeAccessory();
                    }
                }
            }
        };
    }

    protected boolean openAccessory(UsbAccessory usbAccessocd) {
        try {
            mParcelFileDescriptor = mUsbManager.openAccessory(usbAccessory);
            if (mParcelFileDescriptor != null) {
                mUsbAccessory = usbAccessory;
                FileDescriptor fileDescriptor = mParcelFileDescriptor.getFileDescriptor();

                if (fileDescriptor != null) {
                    mFileInputStream = new FileInputStream(fileDescriptor);
                    mFileOutputStream = new FileOutputStream(fileDescriptor);
                    return true;
                }
            }
        } catch(SecurityException e) {
            // Nothing
            Log.e(getClass().getSimpleName(), e.getMessage());
        }
        return false;
    }

    protected void closeAccessory() {
        if (mParcelFileDescriptor != null) {
            try {
                mParcelFileDescriptor.close();
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
            } finally {
                if (mFileInputStream != null) {
                    try {
                        mFileInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (mFileOutputStream != null) {
                    try {
                        mFileOutputStream.flush();
                        mFileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        mFileOutputStream = null;
        mFileInputStream = null;
        mParcelFileDescriptor = null;
        mUsbAccessory = null;
    }

    public boolean serialAvailable() {
        return mByteRead >= 0;
    }

    @Override
    public void write(byte[] values) {
        try {
            mFileOutputStream.write(values);
            mFileOutputStream.flush();
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    @Override
    public void write(byte value) {
        write((int) value);
    }

    @Override
    public void write(int value) {
        try {
            mFileOutputStream.write(value);
            mFileOutputStream.flush();
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    @Override
    public void write(float value) {
        String bufferOfString = String.valueOf(value);
        write(bufferOfString);
    }

    @Override
    public void write(String text) {
        byte[] buffer = text.getBytes();

        try {
            mFileOutputStream.write(buffer);
            mFileOutputStream.flush();
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    @Override
    public AdkMessage read() throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        byte[] response;
        AdkMessage message;

        // Read from ADK
        mByteRead = mFileInputStream.read(buffer, 0, buffer.length);

        if (mByteRead != -1) {
            // Create a new buffer that fits the exact number of read bytes
            response = Arrays.copyOfRange(buffer, 0, mByteRead);

            // Prepare a message instance
            message = new AdkMessage(response);
        } else {
            message = new AdkMessage(null);
        }

        return message;
    }

    @Override
    public void close() {
        closeAccessory();
    }

    @Override
    public boolean open() {
        if (mFileInputStream == null || mFileOutputStream == null) {
            UsbAccessory[] usbAccessoryList = mUsbManager.getAccessoryList();
            if (usbAccessoryList != null && usbAccessoryList.length > 0) {
                return openAccessory(usbAccessoryList[0]);
            }
        }
        return false;
    }

    public IntentFilter getDetachedFilter() {
        return mDetachedFilter;
    }
    public BroadcastReceiver getUsbReceiver() {
        return mUsbReceiver;
    }

    // Protected methods used by tests
    // -------------------------------

    protected void setFileInputStream(FileInputStream fileInputStream) {
        this.mFileInputStream = fileInputStream;
    }

    protected void setFileOutputStream(FileOutputStream fileOutputStream) {
        this.mFileOutputStream = fileOutputStream;
    }

    protected UsbManager getUsbManager() {
        return mUsbManager;
    }

    public boolean isOpen() {
        return mFileInputStream != null && mFileOutputStream != null;
    }
}
