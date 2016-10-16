/* 
 * File:   Record.cpp
 * Author: ettoremaiorana
 * 
 * Created on 01 October 2016, 15:56
 */

#include "Record.h"
#include <iomanip>      // std::get_time
#include <ctime>        // struct std::tm

using namespace std;

Record::Record(const string &line) {
    stringstream parser(line);
    char c1, c2, c3, c4, c5, c6, c7, c8, c9, c10;
    double d1, d2, d3, d4;
    int i5, i6, i7, i8, i9, i10;
    string date, time;
    parser >> d1 >> c1 >> d2 >> c2 >> d3 >> c3 >> d4 >> c4 >> i5 >> c5 >> 
            date >> time;
            
    open = d1;
    high = d2;
    low = d3;
    close = d4;
    volume = i5;
    
    stringstream datetimeParser(date + " " + time);
    datetimeParser >> i6 >> c6 >> i7 >> c7 >> i8 >> c8 >> i9 >> c9 >> i10 >> c10;
    month = i6;
    day = i7;
    year = i8;
    hour = i9;
    minute = i10;

    string datetime = date + " " + time;
    stringstream timestampParser(datetime);
    struct tm when = {0};
    timestampParser >> get_time(&when,"%D %R");
    timestamp = mktime(&when);
    //cout << "Record line constructor: " << when.tm_year << "-" << when.tm_mon << "-" << when.tm_mday << " = " << timestamp <<endl;
}

bool Record::isOnDifferentDate(const Record& r) {
    return day != r.day;
}

bool Record::isOnDifferentHour(const Record& r) {
    return hour != r.hour;
}

unsigned long Record::gettimestamp() {
    return timestamp;
}





