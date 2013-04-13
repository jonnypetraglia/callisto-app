/*
 * Copyright (C) 2012-2013 Qweex
 * This file is a part of Callisto.
 *
 * Callisto is free software; it is released under the
 * Open Software License v3.0 without warranty. The OSL is an OSI approved,
 * copyleft license, meaning you are free to redistribute
 * the source code under the terms of the OSL.
 *
 * You should have received a copy of the Open Software License
 * along with Callisto; If not, see <http://rosenlaw.com/OSL3.0-explained.htm>
 * or check OSI's website at <http://opensource.org/licenses/OSL-3.0>.
 */
package com.qweex.callisto;

import java.io.Serializable;

import com.paypal.android.MEP.PayPalResultDelegate;

public class ResultDelegate implements PayPalResultDelegate, Serializable {

	private static final long serialVersionUID = 10001L;

	/**
	 * Notification that the payment has been completed successfully.
	 * 
	 * @param payKey			the pay key for the payment
	 * @param paymentStatus		the status of the transaction
	 */
	public void onPaymentSucceeded(String payKey, String paymentStatus) {
	}

	/**
	 * Notification that the payment has failed.
	 * 
	 * @param paymentStatus		the status of the transaction
	 * @param correlationID		the correlationID for the transaction failure
	 * @param payKey			the pay key for the payment
	 * @param errorID			the ID of the error that occurred
	 * @param errorMessage		the error message for the error that occurred
	 */
	public void onPaymentFailed(String paymentStatus, String correlationID,
			String payKey, String errorID, String errorMessage) {
	}

	/**
	 * Notification that the payment was canceled.
	 * 
	 * @param paymentStatus		the status of the transaction
	 */
	public void onPaymentCanceled(String paymentStatus) {
	}
}