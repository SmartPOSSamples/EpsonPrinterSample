package com.epson.epos2_printer.printer;

import android.app.Activity;
import android.content.Context;

import com.epson.epos2.Epos2Exception;
import com.epson.epos2.printer.Printer;
import com.epson.epos2.printer.PrinterStatusInfo;
import com.epson.epos2.printer.ReceiveListener;
import com.epson.epos2_printer.App;
import com.epson.epos2_printer.MainActivity;
import com.epson.epos2_printer.R;

/**
 * @author thisfeng
 * @date 2017/12/22-9:59AM
 * <p>
 * Encapsulated printing class
 */

public class ReceiptPrinter implements ReceiveListener {


    private Context mContext = null;
    private Printer mPrinter = null;


    private static ReceiptPrinter instance;

    private ReceiptPrinter() {
    }

    /**
     * This method uses a double lock mechanism,
     * which is safe and can maintain high performance in multi-threaded situations.
     */
    public static ReceiptPrinter getInstance() {
        if (instance == null) {
            synchronized (ReceiptPrinter.class) {
                if (instance == null) {
                    instance = new ReceiptPrinter();
                }
            }
        }
        return instance;
    }

    public void runPrintReceiptSequence(Context context, String datas) {
        mContext = context;
        /* initialize object */
        if (!initializeObject()) {
        }
        /* Create print data */
        if (!createReceiptData(datas)) {
            finalizeObject();
        }
        /*print*/
        if (!printData()) {
            finalizeObject();
        }
    }

    /**
     * Initialize the printing object and return true whether to generate
     */
    private boolean initializeObject() {
        try {
            // ((SpnModelsItem) mSpnLang.getSelectedItem()).getModelConstant()
//            mPrinter = new Printer(((SpnModelsItem) mSpnSeries.getSelectedItem()).getModelConstant(),
            mPrinter = new Printer(Printer.TM_T88, Printer.MODEL_ANK, mContext);
            //

        } catch (Exception e) {
            ShowMsg.showException(e, "Printer", mContext);
            return false;
        }

        mPrinter.setReceiveEventListener(this);

        return true;
    }

    /**
     * Print result monitoring
     */
    @Override
    public void onPtrReceive(final Printer printerObj, final int code, final PrinterStatusInfo status, final String printJobId) {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public synchronized void run() {
                ShowMsg.showResult(code, makeErrorMessage(status), mContext);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //Disconnect current printing after execution
                        disconnectPrinter();
                    }
                }).start();
            }
        });
    }


    private boolean createReceiptData(String datas) {
        String method = "";
//        Bitmap logoData = BitmapFactory.decodeResource(getResources(), R.drawable.store);

        StringBuffer textData = new StringBuffer("BIG5");//Traditional character set BIG5 Chinese GBK


        if (mPrinter == null) {
            return false;
        }
        try {
            //Language requirements
            textData.append(datas);


            mPrinter.addTextLang(Printer.LANG_ZH_TW);

            method = "addTextAlign";

            mPrinter.addTextAlign(Printer.ALIGN_CENTER);


            mPrinter.addTextSize(Printer.PARAM_DEFAULT, Printer.PARAM_DEFAULT);

            mPrinter.addText(datas);

            //2D symbol QR code MODEL_2 can be scanned and recognized. Width 3 to 16, default 3 Height 1～255 Size: 0～65535
            mPrinter.addSymbol("12321421", Printer.SYMBOL_QRCODE_MODEL_2, Printer.PARAM_DEFAULT, 9, 9, 0);
            //skip a line
            mPrinter.addFeedLine(1);
            mPrinter.addText("Online reservation terminal number 2    174.81\n");
            //skip 5 lines
            mPrinter.addFeedLine(5);


/*
            //barcode
             mPrinter.addBarcode("012345678902",
                        Printer.BARCODE_UPC_A,
                        Printer.HRI_BELOW,
                        Printer.FONT_A,
                        2,
                        100); //Width:2 to 6 ; Height:1 to 255
                mPrinter.addBarcode("012345678902",
                        Printer.BARCODE_UPC_E,
                        Printer.HRI_BELOW,
                        Printer.FONT_B,
                        2,
                        100); //Width:2 to 6 ; Height:1 to 255
                mPrinter.addBarcode("012345678902",
                        Printer.BARCODE_EAN13,
                        Printer.HRI_BELOW,
                        Printer.FONT_C,
                        2,
                        100); //Width:2 to 6 ; Height:1 to 255
                mPrinter.addBarcode("0123456",
                        Printer.BARCODE_EAN8,
                        Printer.HRI_BELOW,
                        Printer.FONT_D,
                        2,
                        100); //Width:2 to 6 ; Height:1 to 255
                //BARCODE_CODE39 type number length is 11~15; HRI_BELOW position;
                the font is still the best-looking default size of FONT_A, FONT_C is smaller
                mPrinter.addBarcode("012094578902215",
                        Printer.BARCODE_CODE39,
                        Printer.HRI_BELOW,
                        Printer.FONT_A,
                        2,
                        100); //Width:2 to 6 ; Height:1 to 255
                mPrinter.addText("Liheng sales number" + "\n");
                mPrinter.addFeedLine(1);
                mPrinter.addCut(Printer.CUT_FEED);*/
        } catch (Exception e) {
            ShowMsg.showException(e, method, mContext);
            return false;
        }

        textData = null;

        return true;
    }


    /**
     * Print data
     */
    private boolean printData() {
        if (mPrinter == null) {
            return false;
        }
        //Connect a printing device
        if (!connectPrinter()) {
            return false;
        }
        //Current printing status
        PrinterStatusInfo status = mPrinter.getStatus();

//        dispPrinterWarnings(status);

        //Pop-up prompt when printer is unavailable
        if (!isPrintable(status)) {
            ShowMsg.showMsg(makeErrorMessage(status), mContext);
            try {
                //disconnect
                mPrinter.disconnect();
            } catch (Exception ex) {
                // Do nothing
            }
            return false;
        }

        try {
            //Send data already added in mPrinter via print object
            mPrinter.sendData(Printer.PARAM_DEFAULT);
        } catch (Exception e) {
            ShowMsg.showException(e, "sendData", mContext);
            try {
                mPrinter.disconnect();
            } catch (Exception ex) {
                // Do nothing
            }
            return false;
        }

        return true;
    }

    /**
     * Connect printer
     *
     * @return
     */
    private boolean connectPrinter() {
        boolean isBeginTransaction = false;

        if (mPrinter == null) {
            return false;
        }

        try {
            //Connection USB device address My EPSON TM-T88IV model address: USB:/dev/bus/usb/004/002
            // You must connect the model by opening the search device settings
//            mPrinter.connect(mEditTarget.getText().toString(), Printer.PARAM_DEFAULT);
            mPrinter.connect(MainActivity.getPrinterTarget(), Printer.PARAM_DEFAULT);
        } catch (Exception e) {
            ShowMsg.showException(e, "connect fail", mContext);
            return false;
        }

        try {
            mPrinter.beginTransaction();
            isBeginTransaction = true;
        } catch (Exception e) {
            ShowMsg.showException(e, "beginTransaction", mContext);
        }

        if (isBeginTransaction == false) {
            try {
                mPrinter.disconnect();
            } catch (Epos2Exception e) {
                // Do nothing
                return false;
            }
        }

        return true;
    }


    /**
     * Whether printing is available
     */
    private boolean isPrintable(PrinterStatusInfo status) {
        if (status == null) {
            return false;
        }
        if (status.getConnection() == Printer.FALSE) {
            return false;
        } else if (status.getOnline() == Printer.FALSE) {
            return false;
        } else {
            //print available
        }

        return true;
    }


    /**
     *  current print
     */
    private void disconnectPrinter() {
        if (mPrinter == null) {
            return;
        }

        try {
            mPrinter.endTransaction();
        } catch (final Exception e) {
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public synchronized void run() {
                    ShowMsg.showException(e, "endTransaction", mContext);
                }
            });
        }

        try {
            mPrinter.disconnect();
        } catch (final Exception e) {
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public synchronized void run() {
                    ShowMsg.showException(e, "disconnect", mContext);
                }
            });
        }

        finalizeObject();
    }

    /**
     * Complete printing, clear the command buffer, close and release the print object
     */
    private void finalizeObject() {
        if (mPrinter == null) {
            return;
        }

        mPrinter.clearCommandBuffer();

        mPrinter.setReceiveEventListener(null);

        mPrinter = null;
    }

    /**
     * Error message
     */
    private String makeErrorMessage(PrinterStatusInfo status) {
        String msg = "";

        if (status.getOnline() == Printer.FALSE) {
            msg += mContext.getString(R.string.handlingmsg_err_offline);
        }
        if (status.getConnection() == Printer.FALSE) {
            msg += mContext.getString(R.string.handlingmsg_err_no_response);
        }
        if (status.getCoverOpen() == Printer.TRUE) {
            msg += mContext.getString(R.string.handlingmsg_err_cover_open);
        }
        if (status.getPaper() == Printer.PAPER_EMPTY) {
            msg += mContext.getString(R.string.handlingmsg_err_receipt_end);
        }
        if (status.getPaperFeed() == Printer.TRUE || status.getPanelSwitch() == Printer.SWITCH_ON) {
            msg += mContext.getString(R.string.handlingmsg_err_paper_feed);
        }
        if (status.getErrorStatus() == Printer.MECHANICAL_ERR || status.getErrorStatus() == Printer.AUTOCUTTER_ERR) {
            msg += mContext.getString(R.string.handlingmsg_err_autocutter);
            msg += mContext.getString(R.string.handlingmsg_err_need_recover);
        }
        if (status.getErrorStatus() == Printer.UNRECOVER_ERR) {
            msg += mContext.getString(R.string.handlingmsg_err_unrecover);
        }
        if (status.getErrorStatus() == Printer.AUTORECOVER_ERR) {
            if (status.getAutoRecoverError() == Printer.HEAD_OVERHEAT) {
                msg += mContext.getString(R.string.handlingmsg_err_overheat);
                msg += mContext.getString(R.string.handlingmsg_err_head);
            }
            if (status.getAutoRecoverError() == Printer.MOTOR_OVERHEAT) {
                msg += mContext.getString(R.string.handlingmsg_err_overheat);
                msg += mContext.getString(R.string.handlingmsg_err_motor);
            }
            if (status.getAutoRecoverError() == Printer.BATTERY_OVERHEAT) {
                msg += mContext.getString(R.string.handlingmsg_err_overheat);
                msg += mContext.getString(R.string.handlingmsg_err_battery);
            }
            if (status.getAutoRecoverError() == Printer.WRONG_PAPER) {
                msg += mContext.getString(R.string.handlingmsg_err_wrong_paper);
            }
        }
        if (status.getBatteryLevel() == Printer.BATTERY_LEVEL_0) {
            msg += mContext.getString(R.string.handlingmsg_err_battery_real_end);
        }

        return msg;
    }
}
