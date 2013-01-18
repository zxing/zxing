using System;
using System.Collections.Generic;

/*
 * Copyright 2007 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

namespace com.google.zxing
{


    /// <summary>
    /// <p>Encapsulates the result of decoding a barcode within an image.</p>
    /// 
    /// @author Sean Owen
    /// </summary>
    public sealed class Result
    {

        private readonly string text;
        private readonly sbyte[] rawBytes;
        private ResultPoint[] resultPoints;
        private readonly BarcodeFormat format;
        private IDictionary<ResultMetadataType, object> resultMetadata;
        private readonly long timestamp;

        //public Result(string text, sbyte[] rawBytes, ResultPoint[] resultPoints, BarcodeFormat format)
        //    : this(text, rawBytes, resultPoints, format, System.currentTimeMillis())

        public Result(string text, sbyte[] rawBytes, ResultPoint[] resultPoints, BarcodeFormat format)
            : this(text, rawBytes, resultPoints, format, CurrentTimeMillis())
        {
        }

        public Result(string text, sbyte[] rawBytes, ResultPoint[] resultPoints, BarcodeFormat format, long timestamp)
        {
            this.text = text;
            this.rawBytes = rawBytes;
            this.resultPoints = resultPoints;
            this.format = format;
            this.resultMetadata = null;
            this.timestamp = timestamp;
        }

        /// <returns> raw text encoded by the barcode </returns>
        public string Text
        {
            get
            {
                return text;
            }
        }

        /// <returns> raw bytes encoded by the barcode, if applicable, otherwise {@code null} </returns>
        public sbyte[] RawBytes
        {
            get
            {
                return rawBytes;
            }
        }

        /// <returns> points related to the barcode in the image. These are typically points
        ///         identifying finder patterns or the corners of the barcode. The exact meaning is
        ///         specific to the type of barcode that was decoded. </returns>
        public ResultPoint[] ResultPoints
        {
            get
            {
                return resultPoints;
            }
        }

        /// <returns> <seealso cref="BarcodeFormat"/> representing the format of the barcode that was decoded </returns>
        public BarcodeFormat BarcodeFormat
        {
            get
            {
                return format;
            }
        }

        /// <returns> <seealso cref="Map"/> mapping <seealso cref="ResultMetadataType"/> keys to values. May be
        ///   {@code null}. This contains optional metadata about what was detected about the barcode,
        ///   like orientation. </returns>
        public IDictionary<ResultMetadataType, object> ResultMetadata
        {
            get
            {
                return resultMetadata;
            }
        }

        public void putMetadata(ResultMetadataType type, object value)
        {
            if (resultMetadata == null)
            {
                //resultMetadata = new EnumMap<ResultMetadataType, object>(typeof(ResultMetadataType));
                resultMetadata = new Dictionary<ResultMetadataType, object>();
            }
            resultMetadata[type] = value;
        }

        public void putAllMetadata(IDictionary<ResultMetadataType, object> metadata)
        {
            if (metadata != null)
            {
                if (resultMetadata == null)
                {
                    resultMetadata = metadata;
                }
                else
                {
                    //JAVA TO C# CONVERTER TODO TASK: There is no .NET Dictionary equivalent to the Java 'putAll' method:
                    //resultMetadata.putAll(metadata);
                    foreach (KeyValuePair<ResultMetadataType, object> kvp in metadata)
                    {
                        resultMetadata.Add(kvp);
                    }
                }
            }
        }

        public void addResultPoints(ResultPoint[] newPoints)
        {
            ResultPoint[] oldPoints = resultPoints;
            if (oldPoints == null)
            {
                resultPoints = newPoints;
            }
            else if (newPoints != null && newPoints.Length > 0)
            {
                ResultPoint[] allPoints = new ResultPoint[oldPoints.Length + newPoints.Length];
                Array.Copy(oldPoints, 0, allPoints, 0, oldPoints.Length);
                Array.Copy(newPoints, 0, allPoints, oldPoints.Length, newPoints.Length);
                resultPoints = allPoints;
            }
        }

        public long Timestamp
        {
            get
            {
                return timestamp;
            }
        }

        public override string ToString()
        {
            return text;
        }

        private static readonly DateTime Jan1st1970 = new DateTime
    (1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);

        public static long CurrentTimeMillis()
        {
            return (long)(DateTime.UtcNow - Jan1st1970).TotalMilliseconds;
        }
    }

}