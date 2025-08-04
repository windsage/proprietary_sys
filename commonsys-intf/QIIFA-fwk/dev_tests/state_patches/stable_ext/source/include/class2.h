
/*
	Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved
	Confidential and Proprietary - Qualcomm Technologies, Inc.
*/
#include <iostream>
#include <string>
#include <vector>

namespace qiifa{
	
	enum STATES{
		COMPATIBLE,
		INCOMPATIBLE
	};
	
	struct Component{
		int id;
		std::string name;
	};
	
	// 	'''' 0xC0
	
	class class1{
		public:
			class1();
			std::string generate_lorem_ipsum(unsigned int length);
			int addnums(int x, int y);
			int subnums(int x, int y);
		private:
			std::vector<char> letters;
			
	};
	
}
