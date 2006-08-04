//
// $Id$

package com.threerings.bang.store.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import com.samskivert.util.ListUtil;
import com.threerings.cast.ComponentRepository;
import com.threerings.media.image.ColorPository;
import com.threerings.resource.ResourceManager;
import com.threerings.util.CompiledConfig;

import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Purse;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.data.UnitPass;
import com.threerings.bang.game.data.card.Card;
import com.threerings.bang.server.ServerConfig;

import com.threerings.bang.avatar.util.ArticleCatalog;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.store.data.ArticleGood;
import com.threerings.bang.store.data.CardPackGood;
import com.threerings.bang.store.data.CardTripletGood;
import com.threerings.bang.store.data.Good;
import com.threerings.bang.store.data.PurseGood;
import com.threerings.bang.store.data.UnitPassGood;

import static com.threerings.bang.Log.log;

/**
 * Enumerates the various goods that can be purchased from the General
 * Shop and associates them with providers that are used to actually
 * create and deliver the goods when purchased.
 */
public class GoodsCatalog
{
    /**
     * Creates a goods catalog, loading up the various bits necessary to create
     * articles of clothing and accessories for avatars.
     */
    public GoodsCatalog (AvatarLogic alogic)
    {
        _alogic= alogic;

        // create our goods mappings
        for (int ii = 0; ii < _goods.length; ii++) {
            _goods[ii] = new GoodsMap();
        }

        // register our purses
        ProviderFactory pf = new PurseProviderFactory();
        registerGood(BangCodes.FRONTIER_TOWN, new PurseGood(1, 1000, 1), pf);
        registerGood(BangCodes.INDIAN_POST, new PurseGood(2, 2500, 2), pf);
//         registerGood(BangCodes.BOOM_TOWN, new PurseGood(3, 5000, 4), pf);
//         registerGood(BangCodes.GHOST_TOWN, new PurseGood(4, 7500, 5), pf);
//         registerGood(BangCodes.CITY_OF_GOLD, new PurseGood(5, 15000, 8), pf);

        // register our packs of cards
        pf = new CardProviderFactory();
        for (int ii = 0; ii < PACK_PRICES.length; ii += 3) {
            registerGood(BangCodes.FRONTIER_TOWN,
                         new CardPackGood(PACK_PRICES[ii], PACK_PRICES[ii+1],
                                          PACK_PRICES[ii+2]), pf);
        }
        for (Card card : Card.getCards()) {
            // not all cards are for sale individually
            if (card.getScripCost() <= 0) {
                continue;
            }
            Good good = new CardTripletGood(
                card.getType(), card.getScripCost(), card.getCoinCost());
            registerGood(card.getTownId(), good, pf);
        }

        // load up our avatar article catalog and use the data therein to
        // create goods for all avatar articles
        pf = new ArticleProviderFactory();
        for (ArticleCatalog.Article article :
                 _alogic.getArticleCatalog().getArticles()) {
            ArticleGood good = new ArticleGood(
                article.townId + "/" + article.name, article.scrip,
                article.coins);
            registerGood(article.townId, good, pf);
        }

        // register our unit passes
        pf = new UnitPassProviderFactory();
        UnitConfig[] units = UnitConfig.getTownUnits(ServerConfig.townId);
        for (int ii = 0; ii < units.length; ii++) {
            UnitConfig uc = units[ii];
            if (uc.badgeCode != 0 && uc.scripCost > 0) {
                UnitPassGood good =
                    new UnitPassGood(uc.type, uc.scripCost, uc.coinCost);
                registerGood(ServerConfig.townId, good, pf);
            }
        }
    }

    /**
     * Returns the goods that are available in the town in question.
     */
    public Good[] getGoods (String townId)
    {
        ArrayList<Good> goods = new ArrayList<Good>();
        for (int tt = 0; tt < BangCodes.TOWN_IDS.length; tt++) {
            goods.addAll(_goods[tt].keySet());
            if (BangCodes.TOWN_IDS[tt].equals(townId)) {
                break;
            }
        }
        return goods.toArray(new Good[goods.size()]);
    }

    /**
     * Requests that a {@link Provider} be created to provide the specified
     * good to the specified user. Returns null if no provider is registered
     * for the good in question.
     */
    public Provider getProvider (PlayerObject user, Good good, Object[] args)
        throws InvocationException
    {
        for (int tt = 0; tt < BangCodes.TOWN_IDS.length; tt++) {
            ProviderFactory factory = _goods[tt].get(good);
            if (factory != null) {
                return factory.createProvider(user, good, args);
            }
            if (BangCodes.TOWN_IDS[tt].equals(user.townId)) {
                break;
            }
        }
        return null;
    }

    /**
     * Registers a Good -> ProviderFactory mapping for the specified town.
     */
    protected void registerGood (
        String townId, Good good, ProviderFactory factory)
    {
        int tidx = ListUtil.indexOf(BangCodes.TOWN_IDS, townId);
        if (tidx == -1) {
            log.warning("Requested to register good for invalid town " +
                        "[town=" + townId + ", good=" + good + "].");
            return;
        }
        _goods[tidx].put(good, factory);
    }

    /** Used to create a {@link Provider} for a particular {@link Good}. */
    protected abstract class ProviderFactory {
        public abstract Provider createProvider (
            PlayerObject user, Good good, Object[] args)
            throws InvocationException;
    }

    /** Used for {@link PurseGood}s. */
    protected class PurseProviderFactory extends ProviderFactory {
        public Provider createProvider (
            PlayerObject user, Good good, Object[] args)
            throws InvocationException {
            return new ItemProvider(user, good, args) {
                protected Item createItem () throws InvocationException {
                    int townIndex = ((PurseGood)_good).getTownIndex();
                    return new Purse(_user.playerId, townIndex);
                }
            };
        }

        protected int _townIndex;
    }

    /** Used for {@link CardPackGood}s and {@link CardTripletGood}s. */
    protected class CardProviderFactory extends ProviderFactory {
        public Provider createProvider (
            PlayerObject user, Good good, Object[] args)
            throws InvocationException {
            if (good instanceof CardPackGood) {
                return new CardPackProvider(user, good);
            } else {
                return new CardTripletProvider(user, good);
            }
        }
    }

    /** Used for {@link ArticleGood}s. */
    protected class ArticleProviderFactory extends ProviderFactory {
        public Provider createProvider (
            PlayerObject user, Good good, Object[] args)
            throws InvocationException {
            return new ItemProvider(user, good, args) {
                protected Item createItem () throws InvocationException {
                    ArticleCatalog.Article article =
                        _alogic.getArticleCatalog().getArticle(_good.getType());
                    if (article == null) {
                        log.warning("Requested to create article for unknown " +
                                    "catalog entry [who=" + _user.who() +
                                    ", good=" + _good + "].");
                        throw new InvocationException(
                            InvocationCodes.INTERNAL_ERROR);
                    }
                    // our arguments are colorization ids
                    int zations = AvatarLogic.composeZations(
                        (Integer)_args[0], (Integer)_args[1],
                        (Integer)_args[2]);
                    Item item = _alogic.createArticle(
                        _user.playerId, article, zations);
                    if (item == null) {
                        throw new InvocationException(
                            InvocationCodes.INTERNAL_ERROR);
                    }
                    return item;
                }
            };
        }
    }

    /** Used for {@link UnitPassGood}s. */
    protected class UnitPassProviderFactory extends ProviderFactory {
        public Provider createProvider (
            PlayerObject user, Good good, Object[] args)
            throws InvocationException {
            return new ItemProvider(user, good, args) {
                protected Item createItem () throws InvocationException {
                    String type = ((UnitPassGood)_good).getUnitType();
                    return new UnitPass(_user.playerId, type);
                }
            };
        }
    }

    /** We can't create generic arrays, so we promote to a real class. */
    protected static class GoodsMap extends HashMap<Good,ProviderFactory> {
    }

    /** Handles all of our avatar related bits. */
    protected AvatarLogic _alogic;

    /** Contains mappings from {@link Good} to {@link ProviderFactory} for
     * the goods available in each town. */
    protected GoodsMap[] _goods = new GoodsMap[BangCodes.TOWN_IDS.length];

    /** Quantity, scrip cost and coin cost for our packs of cards. */
    protected static final int[] PACK_PRICES = {
        5, 300, 1,
        13, 750, 2,
        52, 2500, 6,
    };
}
