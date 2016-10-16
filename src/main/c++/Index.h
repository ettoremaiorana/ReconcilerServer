/* 
 * File:   Index.h
 * Author: ettoremaiorana
 *
 * Created on 15 October 2016, 15:49
 */

#ifndef INDEX_H
#define	INDEX_H

#include <map>

class Index {
public:
    Index()=default;
    Index(const std::string &filename, std::map<unsigned long, size_t> offsets);
    const std::string& getId();
    size_t size();
private:
    std::string m_id;
    std::map<unsigned long, size_t> m_offsets; 
};

#endif	/* INDEX_H */

