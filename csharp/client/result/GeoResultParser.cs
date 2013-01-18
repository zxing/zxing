using System;
using System.Text.RegularExpressions;

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

	using Result = com.google.zxing.Result;


	/// <summary>
	/// Parses a "geo:" URI result, which specifies a location on the surface of
	/// the Earth as well as an optional altitude above the surface. See
	/// <a href="http://tools.ietf.org/html/draft-mayrhofer-geo-uri-00">
	/// http://tools.ietf.org/html/draft-mayrhofer-geo-uri-00</a>.
	/// 
	/// @author Sean Owen
	/// </summary>
	public sealed class GeoResultParser : ResultParser
	{

	  private static readonly string GEO_URL_PATTERN = "geo:([\\-0-9.]+),([\\-0-9.]+)(?:,([\\-0-9.]+))?(?:\\?(.*))?";

	  public override ParsedResult parse(Result result)
	  {
		string rawText = getMassagedText(result);

		Match matcher = Regex.Match(rawText,GEO_URL_PATTERN,RegexOptions.IgnoreCase);
		if (!matcher.Success)
		{
		  return null;
		}

		string query = matcher.Groups[4].Value;

		double latitude;
		double longitude;
		double altitude;
		try
		{
		  latitude = Convert.ToDouble(matcher.Groups[1].Value);
		  if (latitude > 90.0 || latitude < -90.0)
		  {
			return null;
		  }
		  longitude = Convert.ToDouble(matcher.Groups[2].Value);
		  if (longitude > 180.0 || longitude < -180.0)
		  {
			return null;
		  }
		  if (matcher.Groups[3].Value == null)
		  {
			altitude = 0.0;
		  }
		  else
		  {
			altitude = Convert.ToDouble(matcher.Groups[3].Value);
			if (altitude < 0.0)
			{
			  return null;
			}
		  }
		}
		catch (FormatException nfe)
		{
		  return null;
		}
		return new GeoParsedResult(latitude, longitude, altitude, query);
	  }

	}
}