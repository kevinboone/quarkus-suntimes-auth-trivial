/*===========================================================================
 
  SunTimesInfo.java

  Copyright (c)2022 Kevin Boone, GPL v3.0

===========================================================================*/

package me.kevinboone.apacheintegration.quarkus_suntimes;
import java.util.Date; 
import java.time.Instant; 
import io.quarkus.runtime.annotations.RegisterForReflection;


/**
  This class is a simple date carrier for the sunrise/sunset information
  calculated by SunTimesBean. It has no particular logic -- SunTimesBean
  write all the information into this class as plain String objects. 

  When this object is returned as a Camel Exchange body, it will be
  converted to JSON by Jackson. In this case, most of the hard work
  has already been done, and all Jackson does for us is format the
  punctuation and escaping of the JSON.

  Each of the getXXX() methods in this class will create a new
  field in the JSON output.
*/
@RegisterForReflection
public class SunTimesInfo
  {
  private String rise = "", set = "", timezone = "", date = "";

  public SunTimesInfo (String timezone, String date, String rise, String set)
    {
    this.rise = rise;
    this.set = set;
    this.timezone = timezone;
    this.date = date;
    }
 
  public String getTimezone()
    { 
    return timezone.replace (":","/");
    }

  public String getDate()
    { 
    return date;
    }

  public String getRise()
    { 
    if (rise == null)
      return "";
    else
      return rise;
    }

  public String getSet()
    { 
    if (set == null)
      return "";
    else
      return set;
    }

  }



