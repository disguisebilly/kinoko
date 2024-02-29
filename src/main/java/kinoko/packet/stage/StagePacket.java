package kinoko.packet.stage;

import kinoko.server.cashshop.CashShop;
import kinoko.server.header.OutHeader;
import kinoko.server.packet.OutPacket;
import kinoko.util.Util;
import kinoko.world.user.User;

import java.time.Instant;

public final class StagePacket {
    // CStage::OnPacket ------------------------------------------------------------------------------------------------

    public static OutPacket setField(User user, int channelId, boolean isMigrate, boolean isRevive) {
        final OutPacket outPacket = OutPacket.of(OutHeader.SET_FIELD);
        outPacket.encodeShort(0); // CClientOptMan::DecodeOpt
        outPacket.encodeInt(channelId); // nChannelID
        outPacket.encodeInt(0); // dwOldDriverID
        outPacket.encodeByte(user.getField().getFieldKey()); // bFieldKey
        outPacket.encodeByte(isMigrate); // bCharacterData
        outPacket.encodeShort(0); // nNotifierCheck

        if (isMigrate) {
            // m_CalcDamage
            final int s1 = Util.getRandom().nextInt();
            final int s2 = Util.getRandom().nextInt();
            final int s3 = Util.getRandom().nextInt();
            outPacket.encodeInt(s1);
            outPacket.encodeInt(s2);
            outPacket.encodeInt(s3);

            // CharacterData::Decode
            user.getCharacterData().encode(outPacket);

            // CWvsContext::OnSetLogoutGiftConfig
            outPacket.encodeInt(0); // bPredictQuit
            outPacket.encodeArray(new byte[4 * 3]); // anLogoutGiftCommoditySN
        } else {
            outPacket.encodeByte(isRevive);
            outPacket.encodeInt(user.getCharacterStat().getPosMap());
            outPacket.encodeByte(user.getCharacterStat().getPortal());
            outPacket.encodeInt(user.getHp());
            outPacket.encodeByte(false); // bChaseEnable -> int, int
        }

        outPacket.encodeFT(Instant.now()); // ftServer
        return outPacket;
    }

    public static OutPacket setCashShop(User user, CashShop cashShop) {
        final OutPacket outPacket = OutPacket.of(OutHeader.SET_CASHSHOP);
        user.getCharacterData().encode(outPacket);

        // CCashShop::LoadData
        outPacket.encodeByte(true); // bCashShopAuthorized
        outPacket.encodeString(user.getAccount().getUsername()); // sNexonClubID
        CashShop.encode(outPacket);
        // ~CCashShop::LoadData

        outPacket.encodeByte(false); // bEventOn
        outPacket.encodeInt(user.getLevel()); // nHighestCharacterLevelInThisAccount
        return outPacket;
    }
}
