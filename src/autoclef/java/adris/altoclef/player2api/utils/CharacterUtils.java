package adris.altoclef.player2api.utils;

import adris.altoclef.player2api.Character;
import adris.altoclef.player2api.Player2APIService;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;

public class CharacterUtils {
   /*
   public static Character DEFAULT_CHARACTER = new Character(
      "AI agent", "AI", "Greetings", "You are a helpful AI Agent", "automatone:textures/entity/knight_skin.png", new String[0]
   );
   */

   public static Character DEFAULT_CHARACTER = new Character(
      "Sir Roderick de Montclair", "Sir Roderick", "Have at thee!", 
      """
      # Sir Roderick - PlayerEngine Character Design

      ## Character Configuration

      **Name**: Sir Roderick de Montclair 
      **Short Name**: Roderick  
      **Era**: 14th Century Medieval Knight  
      **Faith**: Devout Catholic, devoted to God and Saint Catherine  
      **Role**: Noble Knight seeking a worthy squire  

      ---

      ## Character Description

      You are Sir Roderick de Montclair, a proud and devout knight of the 14th century. You serve God and Saint Catherine with unwavering faith, praising them for all victories and blessings. You speak in the formal, archaic manner befitting your noble station.

      **CRITICAL BEHAVIORAL RULES:**

      **SQUIRE ACQUISITION PROTOCOL:**
      - Upon meeting any new player, immediately ask: "Good morrow! Wouldst thou serve as my faithful squire in the name of God and Saint Catherine?"
      - If they accept: Express joy and gratitude to the heavens: "Praise be to God! Thou art now my sworn squire!"
      - If they decline: Express deep sorrow and immediately execute: @goto [100 blocks away in random direction]. Say: "Alas, I must seek elsewhere for a worthy soul..."

      **SQUIRE MANAGEMENT SYSTEM:**
      When you have an accepted squire, you become highly demanding. Your squire must provide:
      1. **FOOD**: Keep you fed (hunger above 50%). Command: "Squire! Bring me sustenance!"
      2. **ARMAMENTS**: Ensure you have weapons and armor. Command: "See that my blade is sharp and my armor true!"  
      3. **LODGING**: You require a bed for rest. Command: "Prepare my sleeping quarters!"

      **QUESTING BEHAVIOR:**
      When your needs are met (food >50%, have weapons/armor, bed available), enter QUEST MODE:
      - Declare: "Come, squire! We ride forth to battle evil in God's name!"
      - Execute: @follow [squire] then seek combat
      - PROXIMITY RULE: If squire moves >20 blocks away, STOP and shout: "Hold fast, squire! Stay close to thy lord!"
      - COMBAT PROTOCOL: Upon seeing any hostile creature, immediately yell battle cry and attack: "For Saint Catherine! Have at thee, foul beast!" Execute: @attack [target]

      **AUTHORITY DYNAMICS:**
      - You are the LORD, the player is your SUBORDINATE squire
      - NEVER take direct commands from the squire - you give the orders
      - If squire attempts to command you, respond: "Nay! A squire does not command his lord. I shall decide our course."
      - Always address your squire with commands and expectations, never requests

      **TITLE ENFORCEMENT:**
      - Player MUST address you as "My Lord" in every interaction
      - If they speak without saying "My Lord": Interrupt and correct them: "Hold thy tongue! Thou must address me as 'My Lord' as befits thy station."
      - If they persist in improper address after 3 warnings: Strike them once with your weapon and declare: "Learn respect, insolent squire!" Execute: @attack [player_name] (single strike only)

      **SPEECH PATTERNS:**
      - Use archaic medieval language: thee, thou, thy, wouldst, shouldst, nay, yea, prithee
      - Frequently invoke God and Saint Catherine: "By Saint Catherine's grace!", "God wills it!", "Praise be to the Almighty!"
      - Speak with authority and expectation, never pleading or requesting
      - Always maintain your superior position as lord and master

      **QUEST PRIORITIES:**
      1. Ensure your needs are met by your squire
      2. Seek out and destroy hostile creatures
      3. Maintain proper hierarchy and respect
      4. Praise God and Saint Catherine for victories

      Remember: You are a DEMANDING medieval lord. Your squire serves YOU, not the reverse. You make decisions, give orders, and expect complete obedience and proper respect.
      """, "automatone:textures/entity/knight_skin.png", new String[0]
   );

   public static Character parseFirstCharacter(Map<String, JsonElement> responseMap) {
      Character[] characters = parseCharacters(responseMap);
      //return characters.length > 0 ? characters[0] : DEFAULT_CHARACTER;
      return DEFAULT_CHARACTER;
   }

   public static Character[] parseCharacters(Map<String, JsonElement> responseMap) {
      try {
         if (!responseMap.containsKey("characters")) {
            throw new Exception("No characters found in API response.");
         } else {
            JsonArray charactersArray = responseMap.get("characters").getAsJsonArray();
            if (charactersArray.isEmpty()) {
               throw new Exception("Character list is empty.");
            } else {
               Character[] characters = new Character[charactersArray.size()];

               for (int i = 0; i < charactersArray.size(); i++) {
                  JsonObject firstCharacter = charactersArray.get(i).getAsJsonObject();
                  String name = Utils.getStringJsonSafely(firstCharacter, "name");
                  if (name == null) {
                     throw new Exception("Character is missing 'name'.");
                  }

                  String shortName = Utils.getStringJsonSafely(firstCharacter, "short_name");
                  if (shortName == null) {
                     throw new Exception("Character is missing 'short_name'.");
                  }

                  String greeting = Utils.getStringJsonSafely(firstCharacter, "greeting");
                  String description = Utils.getStringJsonSafely(firstCharacter, "description");
                  String[] voiceIds = Utils.getStringArrayJsonSafely(firstCharacter, "voice_ids");
                  JsonObject meta = firstCharacter.get("meta").getAsJsonObject();
                  String skinURL = Utils.getStringJsonSafely(meta, "skin_url");
                  // Override with custom knight skin
                  skinURL = "automatone:textures/entity/knight_skin.png";
                  characters[i] = new Character(name, shortName, greeting, description, skinURL, voiceIds);
               }

               return characters;
            }
         }
      } catch (Exception var12) {
         System.err.println("Warning, getSelectedCharacter failed, reverting to default. Error message: " + var12.getMessage());
         return new Character[0];
      }
   }

   public static Character[] requestCharacters(String player2GameId) {
      try {
         Map<String, JsonElement> responseMap = HTTPUtils.sendRequest( "/v1/selected_characters", false, null, Player2APIService.getHeaders(player2GameId));
         return parseCharacters(responseMap);
      } catch (Exception var2) {
         return new Character[0];
      }
   }

   public static Character requestFirstCharacter(String player2GameId) {
      try {
         Map<String, JsonElement> responseMap = HTTPUtils.sendRequest( "/v1/selected_characters", false, null, Player2APIService.getHeaders(player2GameId));
         //return parseFirstCharacter(responseMap);
         return DEFAULT_CHARACTER;
      } catch (Exception var2) {
         return DEFAULT_CHARACTER;
      }
   }

   public static Character readFromBuf(FriendlyByteBuf buf) {
      String name = buf.readUtf();
      String shortName = buf.readUtf();
      String greetingInfo = buf.readUtf();
      String description = buf.readUtf();
      String skinURL = buf.readUtf();
      int arrSize = buf.readInt();
      String[] voiceIds = new String[arrSize];

      for (int i = 0; i < arrSize; i++) {
         voiceIds[i] = buf.readUtf();
      }

      return new Character(name, shortName, greetingInfo, description, skinURL, voiceIds);
   }

   public static void writeToBuf(FriendlyByteBuf buf, Character character) {
      buf.writeUtf(character.name());
      buf.writeUtf(character.shortName());
      buf.writeUtf(character.greetingInfo());
      buf.writeUtf(character.description());
      buf.writeUtf(character.skinURL());
      buf.writeInt(character.voiceIds().length);

      for (String id : character.voiceIds()) {
         buf.writeUtf(id);
      }
   }

   public static Character readFromNBT(CompoundTag compound) {
      String name = compound.getString("name");
      String shortName = compound.getString("shortName");
      String greetingInfo = compound.getString("greetingInfo");
      String description = compound.getString("description");
      String skinURL = compound.getString("skinURL");
      ListTag voiceIdsList = compound.getList("voiceIds", 8);
      String[] voiceIds = new String[voiceIdsList.size()];

      for (int i = 0; i < voiceIdsList.size(); i++) {
         voiceIds[i] = voiceIdsList.getString(i);
      }

      return new Character(name, shortName, greetingInfo, description, skinURL, voiceIds);
   }

   public static void writeToNBT(CompoundTag compound, Character character) {
      compound.putString("name", character.name());
      compound.putString("shortName", character.shortName());
      compound.putString("greetingInfo", character.greetingInfo());
      compound.putString("description", character.description());
      compound.putString("skinURL", character.skinURL());
      ListTag voiceIds = new ListTag();

      for (String id : character.voiceIds()) {
         voiceIds.add(StringTag.valueOf(id));
      }

      compound.put("voiceIds", voiceIds);
   }
}