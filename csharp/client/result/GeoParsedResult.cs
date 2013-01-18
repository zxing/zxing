using System.Text;

/*
 * Copyright 2008 ZXing authors
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

namespace com.google.zxing.client.result
{

	/// <summary>
	/// @author Sean Owen
	/// </summary>
	public sealed class GeoParsedResult : ParsedResult
	{

	  private readonly double latitude;
	  private readonly double longitude;
	  private readonly double altitude;
	  private readonly string query;

	  internal GeoParsedResult(double latitude, double longitude, double altitude, string query) : base(ParsedResultType.GEO)
	  {
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
		this.query = query;
	  }

	  public string GeoURI
	  {
		  get
		  {
			StringBuilder result = new StringBuilder();
			result.Append("geo:");
			result.Append(latitude);
			result.Append(',');
			result.Append(longitude);
			if (altitude > 0)
			{
			  result.Append(',');
			  result.Append(altitude);
			}
			if (query != null)
			{
			  result.Append('?');
			  result.Append(query);
			}
			return result.ToString();
		  }
	  }

	  /// <returns> latitude in degrees </returns>
	  public double Latitude
	  {
		  get
		  {
			return latitude;
		  }
	  }

	  /// <returns> longitude in degrees </returns>
	  public double Longitude
	  {
		  get
		  {
			return longitude;
		  }
	  }

	  /// <returns> altitude in meters. If not specified, in the geo URI, returns 0.0 </returns>
	  public double Altitude
	  {
		  get
		  {
			return altitude;
		  }
	  }

	  /// <returns> query string associated with geo URI or null if none exists </returns>
	  public string Query
	  {
		  get
		  {
			return query;
		  }
	  }

	  public override string DisplayResult
	  {
		  get
		  {
			StringBuilder result = new StringBuilder(20);
			result.Append(latitude);
			result.Append(", ");
			result.Append(longitude);
			if (altitude > 0.0)
			{
			  result.Append(", ");
			  result.Append(altitude);
			  result.Append('m');
			}
			if (query != null)
			{
			  result.Append(" (");
			  result.Append(query);
			  result.Append(')');
			}
			return result.ToString();
		  }
	  }

	}
}