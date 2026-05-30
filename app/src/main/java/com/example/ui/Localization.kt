package com.example.ui

object Localization {
    var currentLanguage = "English" // Can be English or Hindi

    fun getText(key: String): String {
        val isHindi = currentLanguage == "Hindi"
        return when (key) {
            "app_title" -> if (isHindi) "डिबेटमास्टर AI" else "DebateMaster AI"
            "slogan" -> if (isHindi) "बच्चों और छात्रों के लिए आपका सबसे शक्तिशाली भाषण कोच!" else "Your Ultimate AI Speech Coach & Debate Mentor!"
            "toggle_lang" -> if (isHindi) "English में बदलें" else "हिंदी में बदलें"
            "tag_hindi" -> "हिंदी"
            "tag_english" -> "English"
            "select_age" -> if (isHindi) "आयु वर्ग चुनें" else "Select Age Group"
            "select_tone" -> if (isHindi) "भाषण की शैली (Tone)" else "Select Speech Tone"
            "select_len" -> if (isHindi) "भाषण की अवधि (Length)" else "Select Speech Length"
            "topic_label" -> if (isHindi) "विषय दर्ज करें या उत्पन्न करें" else "Enter / Customize Topic"
            "btn_generate_speech" -> if (isHindi) "Arya से भाषण लिखवाएं" else "Generate Speech Script by AI"
            "generated_speech_heading" -> if (isHindi) "Arya द्वारा तैयार भाषण स्क्रिप्ट" else "AI Generated Speech Script"
            "btn_analyze" -> if (isHindi) "भाषण का विस्तृत विश्लेषण करें" else "Analyze Oratory & Logic"
            "hold_to_speak" -> if (isHindi) "बोलने के लिए दबाकर रखें (Hold to Speak)" else "Hold to Speak Speech Input"
            "speech_placeholder" -> if (isHindi) "यहाँ अपना भाषण टाइप करें, या ऊपर माइक का उपयोग करें, या 'चुनें प्रसिद्ध भाषण' से एक उदाहरण लोड करें..." else "Type your speech here, record via the mic above, or load a preset speech below to test-drive..."
            "speech_coach_header" -> if (isHindi) "भाषण कोच" else "Speech Coach & Oratory Evaluator"
            "debate_battle_header" -> if (isHindi) "लाइव डिबेट मुकाबला" else "Live Debate Battle Mode"
            "topic_generator_header" -> if (isHindi) "विषय जनरेटर" else "Infinite Topic & Outline Builder"
            "stats_header" -> if (isHindi) "प्रगति और उपलब्धियां" else "My Progress & Badge Cabinet"
            
            "difficulty" -> if (isHindi) "कठिनाई का स्तर" else "Difficulty Level"
            "difficulty_low" -> if (isHindi) "शुरुआती (Beginner)" else "Beginner"
            "difficulty_mid" -> if (isHindi) "मध्यम (Intermediate)" else "Intermediate"
            "difficulty_high" -> if (isHindi) "विशेषज्ञ (Advanced)" else "Advanced"
            
            "side" -> if (isHindi) "आपका पक्ष" else "Your Debating Side"
            "side_for" -> if (isHindi) "पक्ष में (FOR)" else "FOR / Pro-Topic"
            "side_against" -> if (isHindi) "विपक्ष में (AGAINST)" else "AGAINST / Con-Topic"
            "btn_start_battle" -> if (isHindi) "डिबेट मुकाबला शुरू करें" else "Enter Debate Ring!"
            
            "battle_coaching_hint" -> if (isHindi) " Arya का संकेत (Coaching Hint)" else " Arya's Coach Hint"
            "battle_referee" -> if (isHindi) "रेफरी" else "Referee Board"
            "btn_conclude_battle" -> if (isHindi) "सत्र समाप्त करें और निर्णय लें" else "Conclude and Judge Battle"
            "evaluating" -> if (isHindi) "विश्लेषण जारी है..." else "Evaluating your performance..."
            
            "daily_challenge" -> if (isHindi) "आज का दैनिक भाषण चैलेंज" else "Daily Speaking Challenge!"
            "challenge_topic" -> if (isHindi) "विषय: 'क्या छात्रों के लिए गृहकार्य बंद होना चाहिए?'" else "Topic: 'Should homework be banned for school pupils?'"
            "accept_challenge" -> if (isHindi) "चैलेंज स्वीकार करें!" else "Accept Challenge!"
            "overall_points" -> if (isHindi) "कुल अर्जित बैटल अंक (XP)" else "Total Battle Points (XP)"
            "current_level" -> if (isHindi) "आपका वर्तमान स्तर" else "Current Speaking Level"
            "unlocked_badges" -> if (isHindi) "अनलॉक किए गए मेडल और बैज" else "Unlocked Badge Cabinet"
            "stats_breakdown" -> if (isHindi) "वाक्पटुता कौशल मीटर" else "Orator Skill Metrics"
            
            "fluency" -> if (isHindi) "प्रवाह (Fluency)" else "Fluency"
            "persuasion" -> if (isHindi) "मनाने की क्षमता (Persuasion)" else "Persuasion"
            "vocabulary" -> if (isHindi) "शब्दावली (Vocabulary)" else "Vocabulary"
            "logic" -> if (isHindi) "तार्किक तर्क (Logic)" else "Logic & Reasoning"
            "grammar" -> if (isHindi) "व्याकरण और स्पष्टता" else "Grammar & Clarity"
            
            "load_preset" -> if (isHindi) "प्रसिद्ध भाषण का उदाहरण लोड करें" else "Load A Preset famous Speech Exemplar"
            "preset_vivekananda" -> if (isHindi) "स्वामी विवेकानंद (शिकागो भाषण)" else "Swami Vivekananda (Chicago Speech)"
            "preset_homework" -> if (isHindi) "छात्र गृहकार्य पर छोटा भाषण" else "Short School Speech on Homework"
            "preset_nature" -> if (isHindi) "पर्यावरण संरक्षण भाषण" else "Speech on Saving Nature & Environment"
            
            "battle_points" -> if (isHindi) "पॉइंट्स" else "Pts"
            "streak_count" -> if (isHindi) "लगातार दिन" else "Day Streak"
            
            "generate_topics_btn" -> if (isHindi) "नए वाद-विवाद विषय खोजें" else "Generate 4 Debate Topics"
            "topics_desc" -> if (isHindi) "तथ्य-आधारित पक्ष और विपक्ष तर्कों के साथ तुरंत वाद-विवाद बिंदु प्राप्त करें:" else "Instant brainstorming tool with balanced, fact-based arguments for & against:"
            "category" -> if (isHindi) "श्रेणी चुनें" else "Select Category"
            "cat_edu" -> if (isHindi) "शिक्षा (Education)" else "Education"
            "cat_tech" -> if (isHindi) "तकनीकी और AI" else "Tech & AI"
            "cat_ethics" -> if (isHindi) "नैतिकता (Ethics)" else "Ethics & Value"
            "cat_society" -> if (isHindi) "समाज (Society)" else "Society"
            "cat_sports" -> if (isHindi) "खेल और स्वास्थ्य" else "Sports & Health"
            
            else -> key
        }
    }
}
