
/*
	Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved
	Confidential and Proprietary - Qualcomm Technologies, Inc.
*/
#include <class1.h>

using namespace qiifa;

class1::class1(){
	for(int i = 0; i < 26; i++) class1::letters.push_back('a'+i);
	for(int i = 0; i < 26; i++) class1::letters.push_back('A'+i);
}

std::string class1::generate_lorem_ipsum(unsigned int length){
	unsigned i = 0;
	std::string text = "";
	return text;
}

int class1::addnums(int x, int y){
	return x+y;
}

int class1::subnums(int x, int y){
	return x-y;
}
