//
//  Database.m
//  ZXing
//
//  Created by Christian Brunschen on 29/05/2008.
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

#import "Database.h"
#import "Scan.h"

@implementation Database

static sqlite3_stmt *maxIdStatement;
static sqlite3_stmt *selectAllStatement;
static sqlite3_stmt *insertStatement;
static sqlite3_stmt *deleteStatement;

@synthesize connection;
@synthesize nextScanIdent;

static Database *sharedDatabase = nil;

+ (id)sharedDatabase {
  if (!sharedDatabase) {
    sharedDatabase = [[self alloc] init];
    
    BOOL success;
    NSFileManager *fileManager = [NSFileManager defaultManager];
    NSError *error;
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsDirectory = [paths objectAtIndex:0];
    NSString *writableDBPath = [documentsDirectory stringByAppendingPathComponent:@"scans.db"];
    success = [fileManager fileExistsAtPath:writableDBPath];
    if (!success) {
      // The writable database does not exist, so copy the default to the appropriate location.
      NSString *defaultDBPath = [[[NSBundle mainBundle] resourcePath] stringByAppendingPathComponent:@"scans.db"];
      success = [fileManager copyItemAtPath:defaultDBPath toPath:writableDBPath error:&error];
      if (!success) {
        NSAssert1(0, @"Failed to create writable database file with message '%@'.", [error localizedDescription]);
      }
    }
    
    sqlite3 *connection;
    sqlite3_open([writableDBPath UTF8String], &connection);
    sharedDatabase.connection = connection;
    
    static const char *maxIdSql = "SELECT MAX(id) FROM SCAN";
    sqlite3_prepare_v2(connection, maxIdSql, -1, &maxIdStatement, NULL);
    
    static const char *selectAllSql = "SELECT id, text, stamp FROM SCAN ORDER BY id";
    sqlite3_prepare_v2(connection, selectAllSql, -1, &selectAllStatement, NULL);
    
    static const char *insertSql = 
      "INSERT INTO SCAN (id, text, stamp) VALUES (?, ?, ?)";
    sqlite3_prepare_v2(connection, insertSql, -1, &insertStatement, NULL);
    
    static const char *deleteSql = "DELETE FROM SCAN WHERE id = ?";
    sqlite3_prepare_v2(connection, deleteSql, -1, &deleteStatement, NULL);
    
    if (SQLITE_ROW == sqlite3_step(maxIdStatement)) {
      int maxId = sqlite3_column_int(maxIdStatement, 0);
      sharedDatabase.nextScanIdent = maxId + 1;
      sqlite3_reset(maxIdStatement);
    } else {
      NSLog(@"failed to read max ID from database\n");
    }
    
  }
  return sharedDatabase;
}

- (Scan *)addScanWithText:(NSString *)text {
  NSTimeInterval timeStamp = [[NSDate date] timeIntervalSince1970];
  sqlite3_bind_int(insertStatement, 1, nextScanIdent++);
  sqlite3_bind_text(insertStatement, 2, [text UTF8String], -1, SQLITE_TRANSIENT);
  sqlite3_bind_double(insertStatement, 3, timeStamp);
  sqlite3_step(insertStatement);
  sqlite3_reset(insertStatement);
  NSDate* theDate = [[NSDate alloc] initWithTimeIntervalSince1970:timeStamp];
  Scan *scan = [[[Scan alloc] initWithIdent:nextScanIdent text:text stamp:theDate] autorelease];
  [theDate release];
  return scan;
}

- (NSArray *)scans {
  NSMutableArray *scans = [NSMutableArray array];
  while (SQLITE_ROW == sqlite3_step(selectAllStatement)) {
    int ident = sqlite3_column_int(selectAllStatement, 0);
    NSString *text = [[NSString alloc] initWithUTF8String:(char *)sqlite3_column_text(selectAllStatement, 1)];
    NSDate *stamp = [[NSDate alloc] initWithTimeIntervalSince1970:sqlite3_column_double(selectAllStatement, 2)];
    Scan *scan = [[Scan alloc] initWithIdent:ident text:text stamp:stamp];
    [text release];
    [stamp release];
    [scans addObject:scan];
    [scan release];
  }
  sqlite3_reset(selectAllStatement);
  return scans;
}

- (void)deleteScan:(Scan *)scan {
  sqlite3_bind_int(deleteStatement, 1, [scan ident]);
  sqlite3_step(deleteStatement);
  sqlite3_reset(deleteStatement);
}


@end
