/* 
 * File:   Index.cpp
 * Author: ettoremaiorana
 * 
 * Created on 15 October 2016, 15:49
 */

#include "Index.h"
#include <string>
#include <map>
#include <iostream>

using namespace std;

Index::Index(const std::string& filename, std::map<unsigned long,size_t> offsets) :  m_id(filename), m_offsets(offsets) {
}
size_t Index::size() {
    return m_offsets.size();
}
const string& Index::getId() {
    return m_id;
}

