package games.strategy.triplea.attatchments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * This class is designed to hold common code for holding "conditions". Any attachment that can hold conditions (ie: RulesAttachments), should extend this instead of DefaultAttachment.
 * 
 * @author veqryn [Mark Christopher Duncan]
 * 
 */
public abstract class AbstractConditionsAttachment extends DefaultAttachment implements ICondition
{
	private static final long serialVersionUID = -9008441256118867078L;
	
	protected List<RulesAttachment> m_conditions = new ArrayList<RulesAttachment>(); // list of conditions that this condition can contain
	protected String m_conditionType = "AND"; // m_conditionType modifies the relationship of m_conditions
	protected boolean m_invert = false; // will logically negate the entire condition, including contained conditions
	protected String m_chance = "1:1"; // chance (x out of y) that this action is successful when attempted, default = 1:1 = always successful
	
	public AbstractConditionsAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param conditions
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setConditions(final String conditions) throws GameParseException
	{
		final Collection<PlayerID> playerIDs = getData().getPlayerList().getPlayers();
		for (final String subString : conditions.split(":"))
		{
			RulesAttachment condition = null;
			for (final PlayerID p : playerIDs)
			{
				condition = (RulesAttachment) p.getAttachment(subString);
				if (condition != null)
					break;
				/*try {
					m_conditions = RulesAttachment.get(p, conditionName);
				} catch (IllegalStateException ise) {
				}
				if (m_conditions != null)
					break;*/
			}
			if (condition == null)
				throw new GameParseException("Could not find rule. name:" + subString + thisErrorMsg());
			if (m_conditions == null)
				m_conditions = new ArrayList<RulesAttachment>();
			m_conditions.add(condition);
		}
	}
	
	public List<RulesAttachment> getConditions()
	{
		return m_conditions;
	}
	
	public void clearConditions()
	{
		m_conditions.clear();
	}
	
	public boolean getInvert()
	{
		return m_invert;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setInvert(final String s)
	{
		m_invert = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setConditionType(final String value) throws GameParseException
	{
		String s = value;
		if (s.equalsIgnoreCase("AND") || s.equalsIgnoreCase("OR") || s.equalsIgnoreCase("XOR"))
		{
			s = s.toUpperCase(Locale.ENGLISH);
		}
		else
		{
			final String[] nums = s.split("-");
			if (nums.length == 1)
			{
				if (Integer.parseInt(nums[0]) < 0)
					throw new GameParseException(
								"conditionType must be equal to 'AND' or 'OR' or 'XOR' or 'y' or 'y-z' where Y and Z are valid positive integers and Z is greater than Y" + thisErrorMsg());
			}
			else if (nums.length == 2)
			{
				if (Integer.parseInt(nums[0]) < 0 || Integer.parseInt(nums[1]) < 0 || !(Integer.parseInt(nums[0]) < Integer.parseInt(nums[1])))
					throw new GameParseException(
								"conditionType must be equal to 'AND' or 'OR' or 'XOR' or 'y' or 'y-z' where Y and Z are valid positive integers and Z is greater than Y" + thisErrorMsg());
			}
			else
				throw new GameParseException(
							"conditionType must be equal to 'AND' or 'OR' or 'XOR' or 'y' or 'y-z' where Y and Z are valid positive integers and Z is greater than Y" + thisErrorMsg());
		}
		m_conditionType = s;
	}
	
	public String getConditionType()
	{
		return m_conditionType;
	}
	
	/**
	 * Accounts for Invert and conditionType. Only use if testedConditions has already been filled and this conditions has been tested.
	 */
	public boolean isSatisfied(final HashMap<ICondition, Boolean> testedConditions)
	{
		return isSatisfied(testedConditions, null);
	}
	
	/**
	 * Accounts for Invert and conditionType. IDelegateBridge is not used so can be null, this is because we have already tested all the conditions.
	 */
	public boolean isSatisfied(final HashMap<ICondition, Boolean> testedConditions, final IDelegateBridge aBridge)
	{
		if (testedConditions == null)
			throw new IllegalStateException("testedCondititions can not be null");
		if (testedConditions.containsKey(this))
			return testedConditions.get(this);
		return areConditionsMet(new ArrayList<ICondition>(this.getConditions()), testedConditions, this.getConditionType()) != this.getInvert();
	}
	
	public static Match<AbstractConditionsAttachment> isSatisfiedAbstractConditionsAttachmentMatch(final HashMap<ICondition, Boolean> testedConditions)
	{
		return new Match<AbstractConditionsAttachment>()
		{
			@Override
			public boolean match(final AbstractConditionsAttachment ca)
			{
				return ca.isSatisfied(testedConditions);
			}
		};
	}
	
	/**
	 * Anything that implements ICondition (currently RulesAttachment, TriggerAttachment, and PoliticalActionAttachment)
	 * can use this to get all the conditions that must be checked for the object to be 'satisfied'. <br>
	 * Since anything implementing ICondition can contain other ICondition, this must recursively search through all conditions and contained conditions to get the final list.
	 * 
	 * @param startingListOfConditions
	 * @return
	 * @author veqryn
	 */
	public static HashSet<ICondition> getAllConditionsRecursive(final HashSet<ICondition> startingListOfConditions, HashSet<ICondition> allConditionsNeededSoFar)
	{
		if (allConditionsNeededSoFar == null)
			allConditionsNeededSoFar = new HashSet<ICondition>();
		allConditionsNeededSoFar.addAll(startingListOfConditions);
		for (final ICondition condition : startingListOfConditions)
		{
			for (final ICondition subCondition : condition.getConditions())
			{
				if (!allConditionsNeededSoFar.contains(subCondition))
					allConditionsNeededSoFar.addAll(getAllConditionsRecursive(new HashSet<ICondition>(Collections.singleton(subCondition)), allConditionsNeededSoFar));
			}
		}
		return allConditionsNeededSoFar;
	}
	
	/**
	 * Takes the list of ICondition that getAllConditionsRecursive generates, and tests each of them, mapping them one by one to their boolean value.
	 * 
	 * @param rules
	 * @param data
	 * @return
	 * @author veqryn
	 */
	public static HashMap<ICondition, Boolean> testAllConditionsRecursive(final HashSet<ICondition> rules, HashMap<ICondition, Boolean> allConditionsTestedSoFar, final IDelegateBridge aBridge)
	{
		if (allConditionsTestedSoFar == null)
			allConditionsTestedSoFar = new HashMap<ICondition, Boolean>();
		
		for (final ICondition c : rules)
		{
			if (!allConditionsTestedSoFar.containsKey(c))
			{
				testAllConditionsRecursive(new HashSet<ICondition>(c.getConditions()), allConditionsTestedSoFar, aBridge);
				allConditionsTestedSoFar.put(c, c.isSatisfied(allConditionsTestedSoFar, aBridge));
			}
		}
		
		return allConditionsTestedSoFar;
	}
	
	/**
	 * Accounts for all listed rules, according to the conditionType.
	 * Takes the mapped conditions generated by testAllConditions and uses it to know which conditions are true and which are false. There is no testing of conditions done in this method.
	 * 
	 * @param rules
	 * @param conditionType
	 * @param data
	 * @return
	 * @author veqryn
	 */
	public static boolean areConditionsMet(final List<ICondition> rulesToTest, final HashMap<ICondition, Boolean> testedConditions, final String conditionType)
	{
		boolean met = false;
		if (conditionType.equals("AND"))
		{
			for (final ICondition c : rulesToTest)
			{
				met = testedConditions.get(c);
				if (!met)
					break;
			}
		}
		else if (conditionType.equals("OR"))
		{
			for (final ICondition c : rulesToTest)
			{
				met = testedConditions.get(c);
				if (met)
					break;
			}
		}
		else if (conditionType.equals("XOR"))
		{
			// XOR is confusing with more than 2 conditions, so we will just say that one has to be true, while all others must be false
			boolean isOneTrue = false;
			for (final ICondition c : rulesToTest)
			{
				met = testedConditions.get(c);
				if (isOneTrue && met)
				{
					isOneTrue = false;
					break;
				}
				else if (met)
					isOneTrue = true;
			}
			met = isOneTrue;
		}
		else
		{
			final String[] nums = conditionType.split("-");
			if (nums.length == 1)
			{
				final int start = Integer.parseInt(nums[0]);
				int count = 0;
				for (final ICondition c : rulesToTest)
				{
					met = testedConditions.get(c);
					if (met)
						count++;
				}
				met = (count == start);
			}
			else if (nums.length == 2)
			{
				final int start = Integer.parseInt(nums[0]);
				final int end = Integer.parseInt(nums[1]);
				int count = 0;
				for (final ICondition c : rulesToTest)
				{
					met = testedConditions.get(c);
					if (met)
						count++;
				}
				met = (count >= start && count <= end);
			}
		}
		return met;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setChance(final String chance) throws GameParseException
	{
		final String[] s = chance.split(":");
		try
		{
			final int i = getInt(s[0]);
			final int j = getInt(s[1]);
			if (i > j || i < 1 || j < 1 || i > 120 || j > 120)
				throw new GameParseException("chance should have a format of \"x:y\" where x is <= y and both x and y are >=1 and <=120" + thisErrorMsg());
		} catch (final IllegalArgumentException iae)
		{
			throw new GameParseException("Invalid chance declaration: " + chance + " format: \"1:10\" for 10% chance" + thisErrorMsg());
		}
		m_chance = chance;
	}
	
	/**
	 * @return the number you need to roll to get the action to succeed format "1:10" for 10% chance
	 */
	public String getChance()
	{
		return m_chance;
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		// TODO Auto-generated method stub
	}
}
