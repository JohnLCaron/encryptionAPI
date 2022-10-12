#include "libekm_api.h"
#include "stdio.h"

const char* manifest = "{\"electionScopeId\":\"manifestSimple\",\"contests\":[{\"contestId\":\"contest1\",\"sequenceOrder\":0,\"geopoliticalUnitId\":\"district\",\"numberElected\":1,\"votesAllowed\":1,\"selections\":[{\"selectionId\":\"selection1\",\"sequenceOrder\":0,\"cryptoHash\":\"32858876269172A0404D7AC4D9C4A7EA0A8191F86BADA3827ED7FA673BEEF294\"},{\"selectionId\":\"selection2\",\"sequenceOrder\":1,\"cryptoHash\":\"3F36D6A0FB3F1E36C7AF4AC82DEC3C229CD076443F514E41EE2CFC3858F59A6D\"},{\"selectionId\":\"selection3\",\"sequenceOrder\":2,\"cryptoHash\":\"63E79B5021F78E04FF7CEB9172B3E417DC31ACA2AFB43B7665E0AF54DB7EB508\"}],\"cryptoHash\":\"0DABB10BE61AD207106519BCF484DDE700E7E48E4AA4512941FA9009BB0811CA\"},{\"contestId\":\"contest2\",\"sequenceOrder\":1,\"geopoliticalUnitId\":\"district\",\"numberElected\":1,\"votesAllowed\":1,\"selections\":[{\"selectionId\":\"selection4\",\"sequenceOrder\":3,\"cryptoHash\":\"74A72C7F9C3B6F26E5ECB66D49DA7A6F0660D338CE0203DE78E9918632D0D9D6\"},{\"selectionId\":\"selection5\",\"sequenceOrder\":4,\"cryptoHash\":\"BEA2921EE1187770B1B7DECCEC8DA6606951BA9662BBF6CBCA73D36D62D733ED\"},{\"selectionId\":\"selection6\",\"sequenceOrder\":5,\"cryptoHash\":\"10576CD21B34C0F84701F80973189190ECCBC29DA5FA5BFC89244C4AB1E4B8A7\"}],\"cryptoHash\":\"2AEC1C2F45FA6195281F758514E35DC91BF3C8897B0CA79BDC1CA282FD158A8B\"}],\"ballotStyles\":[{\"ballotStyleId\":\"styling\",\"geopoliticalUnitIds\":[\"district\"]}],\"cryptoHash\":\"1E9FA2E2CB11A649AB0EF19DB9EDC471164DDE90B987243D46564D9183D426F0\"}";
const char* publicKeyBase16String = "63FB52BCD77044AFC99C7BED58E01A338A0956E886C54E8143350B72D879477F5FA35C0DFD1AA9E7BBC16B8306AD37DA4D6347DB4B75D8124F08855F44054E5211B80BE642CB637DCCF36AAA05D7BF1CF2D9E20C05A9F5BDF9F273ED311D66E0AC8E3FF684DA07ED1383C474458356942E6CBCCEC48E562A5C9F199A73DC43DB939CB3E98460BCD149022E630ADF7DB32ECFDD61D1FDD3C241F0B94E1ACE267A8A76F65130201893F43FABE2444B1B503D100411F61A23367B90C1DE11458FBFD4AE3A7ED02FB7B2B39B7744E977A6B37EE2963EDD1D65FB377E810E11A681F148B0D689AC507531623D77FA59A2D478BF03041C3DEBBB22FD3B8A7E500C463FC90651FAABB43A8C1B8CAEB5AA864B0A6E004D497DC0CCF93362E9448653512CC0151E9FED821B98C83697C78C29682C9BD4961BBBDCB7EC8012759833405E4B5C731C996B2E87AD2FC58A9CB3DAE1D03430DE80E8DB29881365BCA82A2B6729889CBC7C04CC11F9BE87DDC603C32B4D8D899E96E6BDE223E8D08C3BCEE8594ECA9C43CC7DB3EC5EF5AB6C4CC87AC4A455452EE9EB801E0B811CAA2D598930CE35CF2DA46D37282B243C94BDD55CF56AB7DD5664B80ADECB1192C7DA8FB9BFBD1B5E1DA36516C3E76AB3F2C88A61CC3B5A5450F3223D9E764851BBBDA2BE54570A8DC21879C487B373E921694FB0E946562F4B12E9B3ACA46C54642A072C299F";
const char* qbarBase16String = "73FFE3A300F89472725727B660737BB582C915C9C6D28EC842F39CD3A3056BB1";

const char* ballot = "{\"ballotId\":\"ballot-id--1699150306\",\"ballotStyleId\":\"styling\",\"contests\":[{\"contestId\":\"contest1\",\"sequenceOrder\":0,\"selections\":[{\"selectionId\":\"selection1\",\"sequenceOrder\":0,\"vote\":1,\"extendedData\":null}]},{\"contestId\":\"contest2\",\"sequenceOrder\":1,\"selections\":[{\"selectionId\":\"selection4\",\"sequenceOrder\":3,\"vote\":0,\"extendedData\":null},{\"selectionId\":\"selection5\",\"sequenceOrder\":4,\"vote\":0,\"extendedData\":null},{\"selectionId\":\"selection6\",\"sequenceOrder\":5,\"vote\":1,\"extendedData\":null}]}]}";
const char *codeSeedBase16String = "C035EC4D454E27E35217D4694BEB87150AAF68C4071EBA94DAD35203DD21EDF5";

int main(int argc, char** argv) {
  //obtain reference for calling Kotlin/Native functions
  libekm_ExportedSymbols* lib = libekm_symbols();

  libekm_KBoolean result = lib->kotlin.root.electionguard.api.setElectionApi(manifest, publicKeyBase16String, qbarBase16String);
  printf("setElectionApi returned %s\n", result ? "ok" :"fail");

  const char* resultEncrypt = lib->kotlin.root.electionguard.api.encryptApi(ballot, codeSeedBase16String);
  printf("encryptApi returned %s\n", resultEncrypt);

  return 0;
}