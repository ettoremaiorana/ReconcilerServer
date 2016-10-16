/* 
 * File:   Record.h
 * Author: ettoremaiorana
 *
 * Created on 01 October 2016, 15:56
 */

#ifndef RECORD_H
#define	RECORD_H

#include <iostream>
#include <sstream>
#include <fstream>
#include <string>

using namespace std;

class Record {
public:
//    Record(double open, double high, double low, double close, int volume, int month, int day, int year, int hour, int minute);
    Record(const string& line);
    Record()=default;
    Record(const Record &record)=default;
    Record(Record &&record)=default;
    ~Record()=default;
    Record& operator =(const Record&)=default;
    Record& operator =(Record&&)=default;
    
    
    bool isOnDifferentDate(const Record &r);
    bool isOnDifferentHour(const Record &r);
    unsigned long gettimestamp();

    friend ostream& operator<<(ostream& os, Record &r) {
        os << r.open << ',' << r.high << ',' << r.low << ',' << r.close << ' ' << 
                r.month << '/' << r.day << '/' << r.year << ' ' << r.hour << ':' << r.minute;
        return os;
    }
    
private:
    unsigned long timestamp;
    double close;
    double open;
    double high;
    double low;
    unsigned short volume;
    unsigned short day;
    unsigned short month;
    unsigned short year;
    unsigned short hour;
    unsigned short minute;
};

#endif	/* RECORD_H */

