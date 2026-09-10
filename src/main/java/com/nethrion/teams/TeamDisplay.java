package com.nethrion.teams;
import net.kyori.adventure.text.*;import net.kyori.adventure.text.format.*;import org.bukkit.entity.Player;import org.bukkit.plugin.java.JavaPlugin;
public final class TeamDisplay {
 private final JavaPlugin plugin;private final TeamManager manager;private final boolean enabled;
 public TeamDisplay(JavaPlugin plugin,TeamManager manager){this.plugin=plugin;this.manager=manager;enabled=plugin.getConfig().getBoolean("settings.tablist-tag-enabled",true);}
 public void start(){} public void stop(){} public void refreshAll(){for(Player p:plugin.getServer().getOnlinePlayers())refresh(p);}
 public void refresh(Player p){if(!enabled)return;Team t=manager.currentTeam(p);if(t==null)return;Component name=gradient(t.getName(),t.getPrimary(),t.getSecondary()).decorate(TextDecoration.BOLD);p.playerListName(Component.text(p.getName()+" ").color(NamedTextColor.GRAY).append(Component.text("┃ ").color(NamedTextColor.DARK_GRAY)).append(name));}
 private Component gradient(String s,String a,String b){TextColor c1=TextColor.fromHexString(a),c2=TextColor.fromHexString(b);if(c1==null||c2==null)return Component.text(s);TextComponent.Builder out=Component.text();int n=Math.max(1,s.length()-1);for(int i=0;i<s.length();i++){float f=(float)i/n;int r=(int)(c1.red()+(c2.red()-c1.red())*f),g=(int)(c1.green()+(c2.green()-c1.green())*f),bl=(int)(c1.blue()+(c2.blue()-c1.blue())*f);out.append(Component.text(String.valueOf(s.charAt(i))).color(TextColor.color(r,g,bl)));}return out.build();}
 public void refreshIfOnline(java.util.UUID u){Player p=plugin.getServer().getPlayer(u);if(p!=null)refresh(p);}
}