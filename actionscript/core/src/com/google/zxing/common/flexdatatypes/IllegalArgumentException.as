package com.google.zxing.common.flexdatatypes
{
	public class IllegalArgumentException extends Error
	{
		public function IllegalArgumentException(message:String="")
		{
			super("IllegalArgumentException"+message);
		}

	}
}